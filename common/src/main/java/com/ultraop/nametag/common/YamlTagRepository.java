package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.*;
import com.ultraop.nametag.core.validation.TagValidator;

import java.nio.file.Path;
import java.util.*;

public final class YamlTagRepository implements TagRepository {
    private final YamlFileStore store;
    private final Map<TagId, Tag> values = new LinkedHashMap<>();

    public YamlTagRepository(Path file) {
        store = new YamlFileStore(file);
        load();
    }

    @Override public synchronized Optional<Tag> find(TagId id) { return Optional.ofNullable(values.get(id)); }
    @Override public synchronized Collection<Tag> findAll() { return List.copyOf(values.values()); }

    @Override public synchronized void save(Tag tag) {
        TagValidator.validate(tag);
        values.put(tag.id(), tag);
        persist();
    }

    @Override public synchronized void delete(TagId id) {
        values.remove(id);
        persist();
    }

    private void load() {
        Map<String,Object> doc = store.load();
        Object raw = doc.get("tags");
        if (raw == null) return;
        for (Map.Entry<String,Object> e : requireMap(raw, "tags").entrySet()) {
            Tag tag = decodeTag(e.getKey(), requireMap(e.getValue(), "tag " + e.getKey()));
            values.put(tag.id(), tag);
        }
    }

    private void persist() {
        Map<String,Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", YamlFileStore.CURRENT_SCHEMA_VERSION);
        Map<String,Object> tags = new LinkedHashMap<>();
        for (Tag tag : values.values()) tags.put(tag.id().value(), encodeTag(tag));
        doc.put("tags", tags);
        store.save(doc);
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
        Map<String,String> config=new LinkedHashMap<>();
        rawConfig.forEach((k,v)->config.put(k,String.valueOf(v)));
        Map<String,Object> rawMeta=requireMap(m.get("metadata"),"metadata");
        Map<String,String> meta=new LinkedHashMap<>();
        rawMeta.forEach((k,v)->meta.put(k,String.valueOf(v)));
        Tag tag=new Tag(new TagId(id), string(m,"displayName"), decodeColor(requireMap(m.get("color"),"color")),
                style,new TagEffect(string(e,"id"),config),integer(m,"priority"),bool(m,"enabled"),
                bool(m,"chatEnabled"),meta);
        TagValidator.validate(tag);
        return tag;
    }

    private static TagColor decodeColor(Map<String,Object> m) {
        return switch (string(m,"type").toLowerCase(Locale.ROOT)) {
            case "preset" -> new TagColor.Preset(string(m,"name"));
            case "rgb" -> new TagColor.Rgb(integer(m,"red"),integer(m,"green"),integer(m,"blue"));
            case "random" -> new TagColor.Random();
            case "gradient" -> new TagColor.Gradient(requireRgb(m.get("start")),requireRgb(m.get("end")));
            default -> throw new IllegalArgumentException("Unknown color type: " + m.get("type"));
        };
    }

    private static TagColor.Rgb requireRgb(Object value) {
        TagColor c=decodeColor(requireMap(value,"gradient stop"));
        if (!(c instanceof TagColor.Rgb rgb)) throw new IllegalArgumentException("Gradient stop must be RGB");
        return rgb;
    }

    private static boolean bool(Map<String,Object> m,String k){ Object v=m.get(k); if(!(v instanceof Boolean b)) throw new IllegalArgumentException("Expected boolean: "+k); return b; }
    private static int integer(Map<String,Object> m,String k){ Object v=m.get(k); if(!(v instanceof Number n)) throw new IllegalArgumentException("Expected number: "+k); return n.intValue(); }
    private static String string(Map<String,Object> m,String k){ Object v=m.get(k); if(v==null) throw new IllegalArgumentException("Missing value: "+k); return String.valueOf(v); }
    private static Map<String,Object> requireMap(Object v,String name){
        if(!(v instanceof Map<?,?> raw)) throw new IllegalArgumentException("Expected map: "+name);
        Map<String,Object> r=new LinkedHashMap<>();
        for(Map.Entry<?,?> e:raw.entrySet()){ if(!(e.getKey() instanceof String k)) throw new IllegalArgumentException("Expected string key: "+name); r.put(k,e.getValue()); }
        return r;
    }
}
