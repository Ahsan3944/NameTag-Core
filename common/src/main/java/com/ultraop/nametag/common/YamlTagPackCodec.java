package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.*;
import com.ultraop.nametag.core.validation.TagValidator;

import java.nio.file.Path;
import java.util.*;

public final class YamlTagPackCodec {
    private YamlTagPackCodec() {}

    public static void exportTo(Path file, Collection<Tag> tags) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(tags, "tags");
        Map<String,Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", YamlFileStore.CURRENT_SCHEMA_VERSION);
        doc.put("format", "nametag-tag-pack");
        Map<String,Object> rawTags = new LinkedHashMap<>();
        for (Tag tag : tags) {
            TagValidator.validate(tag);
            rawTags.put(tag.id().value(), encodeTag(tag));
        }
        doc.put("tags", rawTags);
        new YamlFileStore(file).save(doc);
    }

    public static int importInto(Path file, TagRepository repository) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(repository, "repository");
        Map<String,Object> doc = new YamlFileStore(file).load();
        if (!"nametag-tag-pack".equals(String.valueOf(doc.get("format")))) {
            throw new IllegalArgumentException("Not a NameTag tag pack: " + file.getFileName());
        }
        Object raw = doc.get("tags");
        if (!(raw instanceof Map<?,?> map)) {
            throw new IllegalArgumentException("Tag pack contains no tags");
        }
        int count = 0;
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String id)) {
                throw new IllegalArgumentException("Tag pack IDs must be strings");
            }
            Tag tag = decodeTag(id, requireMap(entry.getValue(), "tag " + id));
            repository.save(tag);
            count++;
        }
        return count;
    }

    private static Map<String,Object> encodeTag(Tag tag) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("displayName", tag.displayName());
        r.put("color", encodeColor(tag.color()));
        Map<String,Object> s = new LinkedHashMap<>();
        s.put("bold", tag.style().bold()); s.put("italic", tag.style().italic());
        s.put("underlined", tag.style().underlined()); s.put("strikethrough", tag.style().strikethrough());
        s.put("obfuscated", tag.style().obfuscated()); r.put("style", s);
        Map<String,Object> e = new LinkedHashMap<>();
        e.put("id", tag.effect().id()); e.put("configuration", tag.effect().configuration());
        r.put("effect", e);
        r.put("priority", tag.priority()); r.put("enabled", tag.enabled());
        r.put("chatEnabled", tag.chatEnabled()); r.put("metadata", tag.metadata());
        return r;
    }

    private static Map<String,Object> encodeColor(TagColor c) {
        Map<String,Object> r = new LinkedHashMap<>();
        if (c instanceof TagColor.Preset p) { r.put("type","preset"); r.put("name",p.name()); }
        else if (c instanceof TagColor.Rgb x) { r.put("type","rgb"); r.put("red",x.red()); r.put("green",x.green()); r.put("blue",x.blue()); }
        else if (c instanceof TagColor.Random) r.put("type","random");
        else if (c instanceof TagColor.Gradient g) { r.put("type","gradient"); r.put("start",encodeColor(g.start())); r.put("end",encodeColor(g.end())); }
        else throw new IllegalArgumentException("Unsupported color type: " + c.getClass());
        return r;
    }

    private static Tag decodeTag(String id, Map<String,Object> m) {
        Map<String,Object> s=requireMap(m.get("style"),"style");
        TagStyle style=new TagStyle(bool(s,"bold"),bool(s,"italic"),bool(s,"underlined"),bool(s,"strikethrough"),bool(s,"obfuscated"));
        Map<String,Object> e=requireMap(m.get("effect"),"effect");
        Map<String,Object> rawConfig=requireMap(e.get("configuration"),"effect.configuration");
        Map<String,String> config=new LinkedHashMap<>(); rawConfig.forEach((k,v)->config.put(k,String.valueOf(v)));
        Map<String,Object> rawMeta=requireMap(m.get("metadata"),"metadata");
        Map<String,String> meta=new LinkedHashMap<>(); rawMeta.forEach((k,v)->meta.put(k,String.valueOf(v)));
        Tag tag=new Tag(new TagId(id),string(m,"displayName"),decodeColor(requireMap(m.get("color"),"color")),
                style,new TagEffect(string(e,"id"),config),integer(m,"priority"),bool(m,"enabled"),
                bool(m,"chatEnabled"),meta);
        TagValidator.validate(tag);
        return tag;
    }

    private static TagColor decodeColor(Map<String,Object> m) {
        return switch(string(m,"type").toLowerCase(Locale.ROOT)) {
            case "preset" -> new TagColor.Preset(string(m,"name"));
            case "rgb" -> new TagColor.Rgb(integer(m,"red"),integer(m,"green"),integer(m,"blue"));
            case "random" -> new TagColor.Random();
            case "gradient" -> new TagColor.Gradient(requireRgb(m.get("start")),requireRgb(m.get("end")));
            default -> throw new IllegalArgumentException("Unknown color type: "+m.get("type"));
        };
    }
    private static TagColor.Rgb requireRgb(Object value) {
        TagColor color=decodeColor(requireMap(value,"gradient stop"));
        if (!(color instanceof TagColor.Rgb rgb)) throw new IllegalArgumentException("Gradient stop must be RGB");
        return rgb;
    }
    private static boolean bool(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof Boolean b))throw new IllegalArgumentException("Expected boolean: "+k);return b;}
    private static int integer(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof Number n))throw new IllegalArgumentException("Expected number: "+k);return n.intValue();}
    private static String string(Map<String,Object> m,String k){Object v=m.get(k);if(v==null)throw new IllegalArgumentException("Missing value: "+k);return String.valueOf(v);}
    private static Map<String,Object> requireMap(Object v,String name){
        if(!(v instanceof Map<?,?> raw))throw new IllegalArgumentException("Expected map: "+name);
        Map<String,Object> out=new LinkedHashMap<>();
        for(Map.Entry<?,?> e:raw.entrySet()){if(!(e.getKey() instanceof String key))throw new IllegalArgumentException("Expected string key: "+name);out.put(key,e.getValue());}
        return out;
    }
}
