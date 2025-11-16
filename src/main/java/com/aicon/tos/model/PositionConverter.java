package com.aicon.tos.model;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.avlino.common.Constants;
import com.avlino.common.KeyValue;
import com.avlino.common.datasources.SqlConfigReader;
import com.avlino.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.avlino.common.Constants.COLON;
import static com.avlino.common.Constants.REGEX_SLASH;

/**
 * Splits, merges and converts values from Aicon position names/ids to Tos names/ids and vv.
 * Although things are configured, not all need to be, a lot can be derived from the TOS-db, like the position schema.
 */
public class PositionConverter {
    private static final Logger LOG = LoggerFactory.getLogger(PositionConverter.class);

    public static final String POS_PART_SEP     = Constants.DASH;
    public static final String POS_KEY_SEP      = "pos.sep";
    public static final String POS_PART_TYPE    = "pos.type";
    public static final String POS_PART_YARD    = "yard.id";
    public static final String POS_PART_BLOCK   = "block.id";
    public static final String POS_PART_ROW     = "row.id";
    public static final String POS_PART_COLUMN  = "column.id";
    public static final String POS_PART_TIER    = "tier.id";

    static final String POS_TYPE_YARD           = "Y";      // not supported yet: R, T, V
    static final String UNKNOWN_YARD_ID         = "???";    // set when not configured

    /** Mapping of characters found in the position schema towards our names */
    private static final String[][] POS_MAP     = {
            {"B", POS_PART_BLOCK},
            {"R", POS_PART_ROW},
            {"C", POS_PART_COLUMN},
            {"T", POS_PART_TIER}
    };

    static final String CFG_GRP_CONVERSIONS = "conversions";
    static final String CFG_KV_SPLIT = "->";
    static final String KEY_CONV_MIN = "minus";
    static final String KEY_CONV_DIV = "divide";
    static final String KEY_CONV_BLOCK = "block.conversion";
    static final String KEY_CONV_ROW = "row.conversion";
    static final String KEY_CONV_COLUMN = "bay.conversion";
    static final String KEY_CONV_TIER = "tier.conversion";

    static List<KeyValue> _posSchemaParts = null;
    private static String _posSchema = null;
    private static String _posSep = POS_PART_SEP;

    static ConfigGroup _conversionGrp = null;
    static Map<String,Map<String,Object>> _posTos2Aicon = null;
    static Map<String,Map<String,Object>> _posAicon2Tos = null;


    static public void setPositionSep(String posSep) {
        _posSep = posSep;   
    }


    /**
     * Collects the individual position parts and puts them in a map with keys like {@link #POS_PART_BLOCK}.
     * @param block the block-id
     * @param row the row-id (aka bay in aicon)
     * @param column the column-id (aka row in aicon)
     * @param tier the tier
     * @return the map with all parts != null
     */
    public static Map<String,String> collectParts(
            String block,
            String row,
            String column,
            String tier
    ) {
        Map<String,String> parts = new HashMap<>();
        if (block   != null) {parts.put(POS_PART_BLOCK  , block);}
        if (row     != null) {parts.put(POS_PART_ROW    , row);}
        if (column  != null) {parts.put(POS_PART_COLUMN , column);}
        if (tier    != null) {parts.put(POS_PART_TIER   , tier);}
        return parts;
    }

    /**
     * Merges the position parts to a single position string, obeying the schema found in the configuration using
     * ConfigItem {@link ConfigDomain#CFG_TOS_POSITION_SCHEMA} when set and valid (else defaults to BRCT format).
     *
     * @param withTypeAndYard will add prefix [type]-[yardId]- to the position
     * @param inSplitFormat will seperate all parts with - like M03-E-07
     * @return the position as a single string obeying the
     */
    public static String mergePosition(
            Map<String,String> fromParts,
            boolean withTypeAndYard,
            boolean inSplitFormat
    ) {
        // Build up position parts list with all individual elements incl. separators.
        List<String> toParts = new ArrayList<>();

        if (withTypeAndYard) {
            String yardId = fromParts.get(POS_PART_YARD);
            if (!StringUtils.hasContent(yardId)) {
                yardId = ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_YARD);
            }
            toParts = new ArrayList<>(List.of(POS_TYPE_YARD, _posSep, yardId != null ? yardId : UNKNOWN_YARD_ID, _posSep));
        }

        List<KeyValue> schemaParts = getPositionSchema();
        if (schemaParts.size() > 0) {
            int idx = 0;
            for (KeyValue kv : schemaParts) {
                switch (kv.key()) {
                    case POS_PART_BLOCK -> toParts.add(fromParts.get(POS_PART_BLOCK));
                    case POS_PART_ROW   -> toParts.add(fromParts.get(POS_PART_ROW));
                    case POS_PART_COLUMN-> toParts.add(fromParts.get(POS_PART_COLUMN));
                    case POS_PART_TIER  -> toParts.add(fromParts.get(POS_PART_TIER));
                    case POS_KEY_SEP    -> {if (!inSplitFormat) toParts.add(String.valueOf(kv.value()));}
                }
                // add separator when in split format && not the last part
                if (!POS_KEY_SEP.equals(kv.key()) && inSplitFormat && idx < schemaParts.size() - 1) {
                    toParts.add(_posSep);
                }
                idx++;
            }
        } else if (inSplitFormat) {
            toParts = List.of(
                    fromParts.get(POS_PART_BLOCK), _posSep,
                    fromParts.get(POS_PART_ROW), _posSep,
                    fromParts.get(POS_PART_COLUMN), _posSep,
                    fromParts.get(POS_PART_TIER));                      // default sequence of separated parts
        } else {
            toParts = List.of(
                    fromParts.get(POS_PART_BLOCK),
                    fromParts.get(POS_PART_ROW),
                    fromParts.get(POS_PART_COLUMN),
                    fromParts.get(POS_PART_TIER));                      // default sequence of parts
        }

        StringBuilder sb = new StringBuilder();
        for (String part: toParts) {
            sb.append(part);
        }

        return sb.toString();
    }

    /**
     * Splits a position into its parts using the general config item {@link ConfigDomain#CFG_TOS_POSITION_SCHEMA} when
     * given and valid, else it can not parse the hostformat and will return with the elements it could find.
     * The tier part is optional on the position, even when it is given in the schema.
     * @param position the position, may have a prefix like Y-[yard.id]- which will be split as well in separate map entries.
     * @return the LinkedHashMap with the KeyValue's parsed (keeps order of appearance while still allowing random access of its parts).
     * @throws StringIndexOutOfBoundsException something wrong with the position (compared to the schema).
     */
    public static LinkedHashMap<String,String> splitPosition(
            String position
    ) throws StringIndexOutOfBoundsException {
        if (!StringUtils.hasContent(position)) {
            throw new StringIndexOutOfBoundsException(String.format(
                    "Invalid position %s to split (expecting [Y-<yard>-]<hostformat> or Y-<yard>-<block>-<row|col>-<col|row>-<tier>.", position));
        }
        LinkedHashMap<String,String> parsed = null;
        try {
            List<KeyValue> schemaParts = getPositionSchema();
            String hostFormat = null;
            position = position.replace(COLON, Constants.DASH);     // sometimes they use Y-<yard>:<position>
            String[] posToParts = position.split(_posSep);

            parsed = new LinkedHashMap<>();
            if (posToParts.length >= 3) {
                if (!POS_TYPE_YARD.equals(posToParts[0])) {
                    LOG.warn("Found Yard position {} of type {}, only Y is supported", position, posToParts[0]);
                }
                parsed.put(POS_PART_TYPE, posToParts[0]);
                parsed.put(POS_PART_YARD, posToParts[1]);
            }

            if (posToParts.length == 1) {
                // position comes in most likely: <hostformat>[.tier]
                hostFormat = posToParts[0];
            } else if (posToParts.length == 3) {
                // position comes in like: Y-<yardid>-<hostformat>[.tier]
                hostFormat = posToParts[2];
            } else if (posToParts.length >= 3 && posToParts.length <= 6) {
                // already split into its parts. now we copy them into the position parts in the right order.
                int i = 2;
                for (KeyValue kv: schemaParts) {
                    if (!POS_KEY_SEP.equals(kv.key()) && i < posToParts.length) {
                        parsed.put(kv.key(), posToParts[i++]);
                    }
                }
                return parsed;
            } else {
                throw new StringIndexOutOfBoundsException(String.format(
                        "Invalid position %s to split (expecting [Y-<yard>-]<hostformat> or Y-<yard>-<block>-<row|col>-<col|row>-<tier>.", position));
            }

            if (hostFormat != null) {
                int pointer = 0;
                for (KeyValue kv: schemaParts) {
                    if (POS_KEY_SEP.equals(kv.key())) {
                        pointer += kv.valueAsString().length();
                    } else {
                        int len = (Integer)kv.value();
                        if (pointer + len <= hostFormat.length()) {
                            parsed.put(kv.key(), hostFormat.substring(pointer, pointer + len));
                            pointer += len;
                        } else if (!POS_PART_TIER.equals(kv.key())) {       // tier is optional
                            throw new StringIndexOutOfBoundsException(String.format("Mismatch between position %s and it's schema %s, while looking for %s", hostFormat, _posSchema, kv.key()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }
        return parsed;
    }

    public static Map<String,String> convertPosPartsToAicon(Map<String,String> fromMap) {
        parsePosConversions();
        return convertPosParts(_posTos2Aicon, fromMap, false);
    }

    public static Map<String,String> convertPosPartsToTos(Map<String,String> fromMap) {
        parsePosConversions();
        return convertPosParts(_posAicon2Tos, fromMap, true);
    }

    static LinkedHashMap<String,String> convertPosParts(
            Map<String,Map<String,Object>> convMap,
            Map<String,String> fromMap,
            boolean toTos
    ) {
        int cnt = fromMap != null ? fromMap.size() : 0;
        LinkedHashMap<String,String> toMap = new LinkedHashMap<>(cnt);
        if (fromMap != null) {
            for (Map.Entry<String,String> entry: fromMap.entrySet()) {
                toMap.put(entry.getKey(), convertPosPart(entry.getKey(), convMap.get(entry.getKey()), entry.getValue(), toTos));
            }
        }
        return toMap;
    }

    static String convertPosPart(String posPartId, Map<String,Object> convMap, String fromPos, boolean toTos) {
        String toPos = fromPos;
        if (convMap != null && !convMap.isEmpty()) {
            if (convMap.containsKey(KEY_CONV_MIN)) {
                // calculations to be done
                try {
                    int posVal = Integer.parseInt(fromPos);
                    int posMin = (Integer)convMap.get(KEY_CONV_MIN);
                    int posDiv = (Integer)convMap.getOrDefault(KEY_CONV_DIV, 1);
                    int idx = -1;
                    if (toTos && posPartId != null) {
                        for (idx = 0; idx < _posSchemaParts.size(); idx++) {
                            KeyValue kv = _posSchemaParts.get(idx);
                            if (posPartId.equals(kv.key())) {
                                break;
                            }
                        }
                    }
                    String intFormat = idx >= 0 ? String.format("%%0%sd", _posSchemaParts.get(idx).value()) : "%s";
                    if (toTos) {
                        toPos = String.format(intFormat, ((posVal * posDiv) - posMin));
                    } else {
                        double calc = Math.ceil(((double)(posVal + posMin) / (double)posDiv));
                        toPos = String.format(intFormat, (int)calc);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // just a simple lookup table
                toPos = String.valueOf(convMap.get(fromPos));
            }
        }
        return toPos;
    }

    static ConfigGroup getConversionConfig() {
        if (_conversionGrp != null) {
            return _conversionGrp;
        } else {
            return SqlConfigReader.getInstance().getSubGroup(CFG_GRP_CONVERSIONS);
        }
    }

    static void parsePosConversions() {
        if (_posTos2Aicon == null) {
            _posTos2Aicon = new HashMap<>();
            _posAicon2Tos = new HashMap<>();
            ConfigGroup convGrp = getConversionConfig();
            if (convGrp != null) {
                _posTos2Aicon.put(POS_PART_BLOCK, convertToMap(convGrp.getItemValue(KEY_CONV_BLOCK), false));
                _posTos2Aicon.put(POS_PART_ROW, convertToMap(convGrp.getItemValue(KEY_CONV_ROW), false));
                _posTos2Aicon.put(POS_PART_COLUMN, convertToMap(convGrp.getItemValue(KEY_CONV_COLUMN), false));
                _posTos2Aicon.put(POS_PART_TIER, convertToMap(convGrp.getItemValue(KEY_CONV_TIER), false));
                _posAicon2Tos.put(POS_PART_BLOCK, convertToMap(convGrp.getItemValue(KEY_CONV_BLOCK), true));
                _posAicon2Tos.put(POS_PART_ROW, convertToMap(convGrp.getItemValue(KEY_CONV_ROW), true));
                _posAicon2Tos.put(POS_PART_COLUMN, convertToMap(convGrp.getItemValue(KEY_CONV_COLUMN), true));
                _posAicon2Tos.put(POS_PART_TIER, convertToMap(convGrp.getItemValue(KEY_CONV_TIER), true));
            }
        }
    }


    /**
     * Splits the config string into a map of tos-id -> aicon-id or else has Integer values {@link #KEY_CONV_MIN} and
     * optionally {@link #KEY_CONV_DIV} when the conversion needs a division. When there are no numbers, the key will
     * not be present.
     * @param sMap the string with something like 01A->1101,02A,1102 or -1 or -1/2
     * @return the map as explained.
     */
    static LinkedHashMap<String,Object> convertToMap(String sMap, boolean reversed) {
        LinkedHashMap<String,Object> convertedMap = new LinkedHashMap<>();
        if (sMap != null) {
            String[] parts = sMap.split(Constants.COMMA);
            if (parts[0].contains(CFG_KV_SPLIT)) {
                for (String fromTo : parts) {
                    String[] ftParts = fromTo.split(CFG_KV_SPLIT);
                    if (ftParts.length == 2) {
                        convertedMap.put(ftParts[reversed ? 1 : 0].trim(), ftParts[reversed ? 0 : 1].trim());
                    }
                }
            } else {
                try {
                    String[] calc = parts[0].split(REGEX_SLASH);
                    String sMinus = calc[0];
                    convertedMap.put(KEY_CONV_MIN, Integer.valueOf(calc[0]));
                    if (calc.length == 2) {
                        convertedMap.put(KEY_CONV_DIV, Integer.valueOf(calc[1]));
                    }
                } catch (NumberFormatException e) {
                    LOG.error("The conversion {} is incorrect, reason: {}", sMap, e.getMessage());
                }
            }
        }
        return convertedMap;
    }



    /**
     * Splits a position schema given by configuration like B3R2C1.T1 into a list of KeyValue objects where a KeyValue
     * can have an entry of {@link #POS_MAP} or a separator character (like the . in the example) with key {@link #POS_KEY_SEP}.
     * @return the position schema list or an empty list when something failed.
     */
    static List<KeyValue> getPositionSchema() {
        if (_posSchemaParts != null) {
            return _posSchemaParts;
        }
        String posSchema = ConfigDomain.getGeneralItem(ConfigDomain.CFG_TOS_POSITION_SCHEMA);
        try {
            getPositionSchema(posSchema);
        } catch (Exception e) {
            LOG.error("Parsing general config item {}={} failed, reason: {}", ConfigDomain.CFG_TOS_POSITION_SCHEMA, posSchema, e);
        }
        return _posSchemaParts;
    }

    /**
     * Splits a position schema like B3R2C1.T1 into a list of KeyValue objects where a KeyValue can have an entry
     * of {@link #POS_MAP} or a separator character (like the . in the example) with key {@link #POS_KEY_SEP}.
     * Note: this method version is for ease of testing.
     * @return the position schema list or an empty list when something failed.
     */
    static List<KeyValue> getPositionSchema(String posSchema) {
        _posSchema = posSchema;
        _posSchemaParts = new ArrayList<>();
        if (StringUtils.hasContent(posSchema)) {
            String[] chars = posSchema.split(Constants.EMPTY);
            int idx = 0;
            while (idx < posSchema.length() - 1) {              // -1 is important here, because we need to look ahead 1 character
                KeyValue kv = null;
                for (int i = 0; i < POS_MAP.length; i++) {
                    if (POS_MAP[i][0].equals(chars[idx])) {
                        kv = new KeyValue(POS_MAP[i][1], Integer.class, Integer.valueOf(chars[++idx]));     // next char is the length
                        break;
                    }
                }
                if (kv == null) {       // so no position char found but a separator
                    kv = new KeyValue(POS_KEY_SEP  , String.class , chars[idx]);
                }
                _posSchemaParts.add(kv);
                idx++;
            }
        }
        return _posSchemaParts;
    }
}
