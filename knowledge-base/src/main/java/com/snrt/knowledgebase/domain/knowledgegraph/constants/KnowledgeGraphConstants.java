package com.snrt.knowledgebase.domain.knowledgegraph.constants;

public class KnowledgeGraphConstants {

    public static class NodeLabel {
        public static final String ENTITY = "ENTITY";
        public static final String CONCEPT = "CONCEPT";
        public static final String TERM = "TERM";
        public static final String DOCUMENT = "DOCUMENT";
    }

    public static class RelationType {
        public static final String RELATES_TO = "RELATES_TO";
        public static final String IS_A = "IS_A";
        public static final String HAS_PART = "HAS_PART";
        public static final String CONTAINS = "CONTAINS";
        public static final String REFERS_TO = "REFERS_TO";
        public static final String SIMILAR_TO = "SIMILAR_TO";
    }
}
