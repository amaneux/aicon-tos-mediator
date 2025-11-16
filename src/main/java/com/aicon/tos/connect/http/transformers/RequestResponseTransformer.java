package com.aicon.tos.connect.http.transformers;

public interface RequestResponseTransformer {
    String transformRequest(String request, String n4Scope);

    String transformResponse(String response);
}

