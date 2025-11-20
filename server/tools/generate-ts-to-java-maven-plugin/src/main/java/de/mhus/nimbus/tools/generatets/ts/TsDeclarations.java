package de.mhus.nimbus.tools.generatets.ts;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TsDeclarations {
    public static class TsProperty {
        public String name;
        public String type;
        public boolean optional;
        public String visibility; // public | private | protected | undefined
    }

    public static class TsMethodParam {
        public String name;
        public String type;
        public boolean optional;
    }

    public static class TsMethod {
        public String name;
        public List<TsMethodParam> params = new ArrayList<>();
        public String returnType; // may be null for constructors
        public String visibility;
        public boolean isConstructor;
    }

    public static class TsInterface {
        public String name;
        public List<String> extendsList = new ArrayList<>();
        public List<TsProperty> properties = new ArrayList<>();
        public List<TsMethod> methods = new ArrayList<>();
    }

    public static class TsEnum {
        public String name;
        public List<String> values = new ArrayList<>();
    }

    public static class TsTypeAlias {
        public String name;
        public String target; // right-hand side as string
    }

    public static class TsClass {
        public String name;
        public String extendsClass;
        public List<String> implementsList = new ArrayList<>();
        public List<TsProperty> properties = new ArrayList<>();
        public List<TsMethod> methods = new ArrayList<>();
    }
}
