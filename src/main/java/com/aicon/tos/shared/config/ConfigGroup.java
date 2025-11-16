package com.aicon.tos.shared.config;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import javax.xml.XMLConstants;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigGroup {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigGroup.class);

    public static final String ATT_NAME = "name";
    public static final String ATT_REF = "ref";
    private ConfigGroup parent;
    private final List<ConfigGroup> groups;
    private final ConfigType type;
    private String name;
    private String ref = null;
    private final Map<String, ConfigItem> items;

    public ConfigGroup(ConfigType type) {
        this.type = type;
        this.items = new LinkedHashMap<>();
        this.groups = new ArrayList<>();
    }

    public ConfigGroup(ConfigType type, String name) {
        this(type);
        this.name = name;
    }

    public String toString() {
        return toStringWithIndent("");
    }

    private String toStringWithIndent(String indent) {
        StringBuilder childText = new StringBuilder(
                String.format("%sConfigGroup(type=%s, name=%s, ref=%s, #items=%s, #groups=%s)%n", indent, type, name,
                              ref, items.size(), groups.size()));
        for (ConfigGroup group : getChildren()) {
            childText.append(group.toStringWithIndent(indent + "    "));
        }
        return childText.toString();
    }

    /**
     * Creates a ConfigGroup which refers to a name of another ConfigGroup.
     *
     * @param refName the name to refer to.
     * @return this object for single line creation of a reference group.
     */
    public ConfigGroup setRef(String refName) {
        this.ref = refName;
        return this;
    }

    public Object getRef() {
        return ref;
    }

    /**
     * Selects an existing group based on type and optionally a name.
     * Any missing ConfigItems (looking up presets) are added to the found ConfigGroup.
     *
     * @param type         the type to select
     * @param nameObj      the name (as Object, will be translated to a String) to select
     * @param nameOptional when true nameObj may be null (and will look for a group with only type given).
     * @return when found the ConfigGroup
     * @throws ConfigurationException when not found and not allowed to create
     */
    public ConfigGroup select(ConfigType type, Object nameObj, boolean nameOptional) throws ConfigurationException {
        ConfigSettings config = ConfigSettings.getInstance();
        String objName = nameObj == null ? null : String.valueOf(nameObj);
        for (ConfigGroup group : groups) {
            if (group.match(type, objName, nameOptional)) {
                config.addMissingPresetItems(group);
                return group;
            }
        }
        throw new ConfigurationException(
                String.format("Can not find config for type=%s and name=%s", type.name(), objName));
    }

    /**
     * Selects a group based on type and name and creates one with pre-filled values from the presets when missing and
     * allowed to create, so basically ensures you always have a ConfigGroup returned unless you don't allow it to be created.
     *
     * @param type         the type to select
     * @param nameObj      the name (as Object, will be translated to a String) to select
     * @param nameOptional when true nameObj may be null (and will look for a group with only type given).
     * @return the found or created ConfigGroup
     */
    public ConfigGroup ensure(ConfigType type, Object nameObj, boolean nameOptional) {
        try {
            return select(type, nameObj, nameOptional);
        } catch (ConfigurationException e) {
            String objName = nameObj == null ? null : String.valueOf(nameObj);
            ConfigGroup group = new ConfigGroup(type, objName);
            addGroup(group);
            ConfigSettings.getInstance().addMissingPresetItems(group);
            return group;
        }
    }

    private boolean match(ConfigType type, String name, boolean nameOptional) {
        return this.type.equals(type) &&
                ((name == null && nameOptional && this.name == null) || (name != null && name.equals(this.name)));
    }

    public ConfigType getType() {
        return type;
    }

    public boolean isOfType(ConfigType type) {
        return type != null && type == this.type;
    }

    public String getName() {
        return name;
    }

    public Collection<ConfigItem> getItems() {
        return items.values();
    }

    public ConfigGroup getParent() {
        return parent;
    }

    public List<ConfigGroup> getChildren() {
        return groups;
    }

    /**
     * Selects the ref child group matching the type.
     *
     * @param ofType select child for this type.
     * @return the childgroup or null when not found
     */
    public ConfigGroup getReferenceGroup(ConfigType ofType) {
        for (ConfigGroup child : groups) {
            if (child.ref != null && child.isOfType(ofType)) {
                return child;
            }
        }
        return null;
    }


    /**
     * Selects the first ref child group found with its type matching any of the types given
     *
     * @param ofTypes the list of types to match against
     */
    public ConfigGroup getReferenceGroup(ConfigType[] ofTypes) {
        for (ConfigGroup child : groups) {
            if (child.ref != null && matchType(ofTypes, child.getType())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Verify if for every reference an entry exists in the targetParent.
     *
     * @param targetParent the target group to look for a reference.
     * @return null when no errors found, else the errors found as text.
     */
    public String verifyReferences(ConfigGroup targetParent) {
        String errors = null;
        for (ConfigGroup childGroup : getChildren()) {
            ConfigGroup refGroup = childGroup.getReferenceGroup(ConfigType.CONNECTION_TYPES);
            if (refGroup != null) {
                ConfigGroup target = refGroup.getReferredGroup(targetParent);
                if (target == null) {
                    errors = (errors == null ? "" : errors) +
                            String.format("%n    No target found in %s:%s for %s:%s", childGroup.getType(),
                                          childGroup.getName(), refGroup.getType(), refGroup.getRef());
                }
            }
        }
        return errors;
    }


    private boolean matchType(ConfigType[] types, ConfigType matchType) {
        if (types == null || matchType == null) {
            return false;
        }

        for (ConfigType mType : types) {
            if (mType.matches(matchType)) {
                return true;
            }
        }
        return false;
    }

    public ConfigGroup getReferredGroup(ConfigGroup parent) {
        ConfigGroup refGroup = null;
        if (ref != null && parent != null) {
            refGroup = parent.getChildGroup(getType(), ref);
        }
        return refGroup;
    }

    public ConfigGroup getReferencedGroup(String ref, ConfigType type) {
        for (ConfigGroup childGroup : getChildren()) {
            if (childGroup.isOfType(type) && (childGroup.name.equals(ref))) {
                    return childGroup;
            }
        }
        return null;
    }

    public void addGroup(ConfigGroup group) {
        if (group != null) {
            groups.add(group);
            group.setParent(this);
        }
    }

    /**
     * Replaces the existing group with same type and name.
     *
     * @param newGroup the newGroup to replace oldGroup with same name & type
     */
    public void replaceChildGroup(ConfigGroup newGroup) {
        ConfigGroup oldGroup;
        if (newGroup != null) {
            oldGroup = getChildGroup(newGroup.getType(), newGroup.getName());
            for (ConfigGroup child : groups) {
                if (oldGroup.getType().equals(child.getType()) &&
                        (oldGroup.getName() == null || oldGroup.getName().equals(child.getName()))) {
                    break;
                }
            }
            addGroup(newGroup);
        }
    }

    private void setParent(ConfigGroup parentGroup) {
        this.parent = parentGroup;
    }

    public ConfigGroup getChildGroup(ConfigType type) {
        return getChildGroup(type, null);
    }

    public ConfigGroup getChildGroup(String name) {
        if (name == null) {
            return null;
        }
        for (ConfigGroup child : groups) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    public ConfigGroup getChildGroup(ConfigType type, String name) {
        for (ConfigGroup child : groups) {
            if (child.getType().equals(type) && (name == null || name.equals(child.getName()))) {
                return child;
            }
        }
        return null;
    }


    public List<String> getChildrensNamesOrTypes() {
        List<String> names = new ArrayList<>(groups.size());
        for (ConfigGroup group : groups) {
            names.add(group.getNameOrType());
        }
        return names;
    }


    public List<String> getChildrenWithNames(ConfigType connType) {
        List<String> names = new ArrayList<>(groups.size());
        for (ConfigGroup group : groups) {
            if (group.getName() != null && (connType == null || group.isOfType(connType))) {
                names.add(group.getName());
            }
        }
        return names;
    }

    private String getNameOrType() {
        return name != null ? name : String.valueOf(type);
    }

    public ConfigItem findItem(String key) {
        if (key == null) {
            return null;
        }
        return items.get(key);
    }

    public String getItemValue(String itemKey) {
        return getItemValue(itemKey, null);
    }

    public String getItemValue(String itemKey, String defaultValue) {
        ConfigItem item = findItem(itemKey);
        String value = null;
        if (item != null) {
            value = item.value();
        }
        return value != null ? value : defaultValue;
    }

    public void addItem(ConfigItem item) {
        items.put(item.key(), item);
    }


    /**
     * Adds item only when missing
     * @param item the item to add when it is not there yet
     */
    public ConfigGroup ensureItem(ConfigItem item) {
        if (findItem(item.key()) == null) {
            items.put(item.key(), item);
        }
        return this;
    }


    /**
     * Builds up the ConfigGroup tree from file given.
     *
     * @param file the file to read from
     * @return null when everything ok, else an error message.
     */
    public String readXmlFile(File file) {
        try {
            if (file == null) {
                throw new IOException("File is null, nothing read.");
            }
            SAXBuilder sax = new SAXBuilder();
            sax.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sax.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            LOG.info("Reading configuration from file {}", file.getCanonicalPath());
            Document doc = sax.build(file);
            Element root = doc.getRootElement();
            fromXml(root);
        } catch (JDOMException e) {
            return String.format("Problem parsing file %s, reason: %s", file, e);
        } catch (IOException e) {
            return String.format("Problem reading file %s, reason: %s", file, e);
        }
        return null;
    }

    /**
     * Fill in all data from the xml for this ConfigGroup and it's children.
     *
     * @param elm the xml element to start reading from
     */
    public void fromXml(Element elm) {
        // Check of het element een geldige root is
        if (Arrays.stream(ConfigType.values()).noneMatch(ct -> ct.name().equals(elm.getName()))) {
            throw new IllegalArgumentException("Invalid config type for root element in XML: " + elm.getName());
        }

        for (Element child : elm.getChildren()) {
            try {
                if (child.getName().equals(ConfigItem.ELEMENT_NAME)) {
                    ConfigItem item = new ConfigItem(child);
                    addItem(item);
                } else {
                    // Validate the elements children
                    if (Arrays.stream(ConfigType.values()).noneMatch(ct -> ct.name().equals(child.getName()))) {
                        throw new IllegalArgumentException("Invalid config type in XML for child: " + child.getName());
                    }
                    ConfigGroup group = new ConfigGroup(ConfigType.valueOf(child.getName()), child.getAttributeValue(ATT_NAME));
                    group.setRef(child.getAttributeValue(ATT_REF));
                    addGroup(group);
                    group.fromXml(child); // Recurse for nested groups
                }
            } catch (Exception e) {
                LOG.error("Error processing child element {}: {}", child.getName(), e.getMessage(), e);
                throw e; // Gooi de uitzondering opnieuw door voor debugging en tests
            }
        }
    }


    /**
     * Store the tree from this ConfigGroup (so including its children) to the xml file.
     *
     * @param file the file to store to.
     * @return null when stored, else an error message.
     */
    public String storeXmlFile(File file) {
        try {
            if (file == null) {
                throw new IOException("File is null, nothing stored.");
            }
            LOG.info("Storing configuration to file {}", file.getCanonicalPath());

            Document doc = new Document();
            Element rootElement = toXml(null);
            reorderAttributes(rootElement); // Ensure attributes are reordered
            doc.setRootElement(rootElement);

            // Write to file
            try (FileWriter fw = new FileWriter(file)) {
                new XMLOutputter(Format.getPrettyFormat()).output(doc, fw);
            }
        } catch (IOException e) {
            return String.format("Problem writing file %s, reason: %s", file, e.getMessage());
        }
        return null;
    }

    public void reorderAttributes(Element element) {
        // Reorder attributes for this element
        List<Attribute> attributes = new ArrayList<>(element.getAttributes());
        attributes.sort((a1, a2) -> {
            List<String> order = List.of("key", "value", "comment");
            return Integer.compare(order.indexOf(a1.getName()), order.indexOf(a2.getName()));
        });

        element.getAttributes().clear(); // Clear existing attributes
        element.setAttributes(attributes); // Set reordered attributes

        // Reorder recursively for children
        for (Element child : element.getChildren()) {
            reorderAttributes(child);
        }
    }

    /**
     * Writes all data from this ConfigGroup and it's children (recursive) to the element given.
     *
     * @param elm the xml element to use as parent for the new created Group element.
     * @return the new created group element, and linked to the given elm as a child.
     */
    Element toXml(Element elm) {
        Element grpElm = new Element(type.toString());
        if (name != null) {
            grpElm.setAttribute(ATT_NAME, name);
        }
        if (ref != null) {
            grpElm.setAttribute(ATT_REF, ref);
        }
        if (elm != null) {
            elm.addContent(grpElm);
        }
        for (ConfigItem item : items.values()) {
            item.toXml(grpElm);
        }
        for (ConfigGroup group : groups) {
            group.toXml(grpElm);
        }
        return grpElm;
    }

    /**
     * Sets the value for a known item only.
     *
     * @param key the key to set the value for
     */
    public void setItemValue(String key, String value) throws ConfigurationException {
        ConfigItem item = findItem(key);
        if (item == null) {
            throw new ConfigurationException("Can not set value of unknown key " + key);
        }
        item.setValue(value);
    }
}
