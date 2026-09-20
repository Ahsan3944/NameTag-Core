package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.validation.TagValidator;

import java.nio.file.Path;
import java.util.*;

public final class YamlPlayerAssignmentRepository implements PlayerAssignmentRepository {
    private final YamlFileStore store;
    private final Map<UUID,PlayerAssignment> values=new LinkedHashMap<>();

    public YamlPlayerAssignmentRepository(Path file){ store=new YamlFileStore(file); load(); }

    @Override public synchronized Optional<PlayerAssignment> find(UUID id){ return Optional.ofNullable(values.get(id)); }
    @Override public synchronized Collection<PlayerAssignment> findAll(){ return List.copyOf(values.values()); }
    @Override public synchronized void save(PlayerAssignment a){ TagValidator.validate(a); values.put(a.playerUuid(),a); persist(); }
    @Override public synchronized void delete(UUID id){ values.remove(id); persist(); }

    private void load(){
        Object raw=store.load().get("assignments"); if(raw==null)return;
        for(Map.Entry<String,Object> e:requireMap(raw).entrySet()){
            UUID uuid=UUID.fromString(e.getKey()); Map<String,Object> m=requireMap(e.getValue());
            List<TagId> ids=new ArrayList<>(); Object rawIds=m.get("assignedTagIds");
            if(rawIds instanceof List<?> list) for(Object id:list) ids.add(new TagId(String.valueOf(id)));
            else if(rawIds!=null) throw new IllegalArgumentException("assignedTagIds must be a list");
            Object rawActive=m.get("activeTagId"); TagId active=rawActive==null?null:new TagId(String.valueOf(rawActive));
            PlayerAssignment a=new PlayerAssignment(uuid,ids,active); TagValidator.validate(a); values.put(uuid,a);
        }
    }

    private void persist(){
        Map<String,Object> doc=new LinkedHashMap<>(); doc.put("schemaVersion",YamlFileStore.CURRENT_SCHEMA_VERSION);
        Map<String,Object> assignments=new LinkedHashMap<>();
        for(PlayerAssignment a:values.values()){
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("assignedTagIds",a.assignedTagIds().stream().map(TagId::value).toList());
            m.put("activeTagId",a.activeTagId()==null?null:a.activeTagId().value());
            assignments.put(a.playerUuid().toString(),m);
        }
        doc.put("assignments",assignments); store.save(doc);
    }

    private static Map<String,Object> requireMap(Object v){
        if(!(v instanceof Map<?,?> raw)) throw new IllegalArgumentException("Expected YAML map");
        Map<String,Object> r=new LinkedHashMap<>();
        for(Map.Entry<?,?> e:raw.entrySet()){ if(!(e.getKey() instanceof String k)) throw new IllegalArgumentException("Expected string YAML key"); r.put(k,e.getValue()); }
        return r;
    }
}
