package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;

import java.util.Collection;

final class TagRepositoryBackedTagService implements com.ultraop.nametag.api.TagRepository {
    private final TagService service;
    TagRepositoryBackedTagService(TagService service){this.service=service;}
    public java.util.Optional<Tag> find(TagId id){return service.find(id);}
    public Collection<Tag> findAll(){return service.list();}
    public void save(Tag tag){if(service.find(tag.id()).isPresent())service.update(tag);else service.create(tag);}
    public void delete(TagId id){service.delete(id);}
}
