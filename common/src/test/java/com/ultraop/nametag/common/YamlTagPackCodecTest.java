package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class YamlTagPackCodecTest {
 @TempDir Path temp;
 @Test void roundTripsAllSupportedTagProperties(){
  Tag tag=new Tag(new TagId("owner"),"OWNER",new TagColor.Gradient(new TagColor.Rgb(1,2,3),new TagColor.Rgb(4,5,6)),
    new TagStyle(true,false,true,false,true),new TagEffect("custom",Map.of("x","y")),42,true,false,Map.of("role","founder"));
  InMemoryTagRepository repo=new InMemoryTagRepository();
  Path file=temp.resolve("pack.yml");
  YamlTagPackCodec.exportTo(file,List.of(tag));
  assertEquals(1,YamlTagPackCodec.importInto(file,repo));
  assertEquals(tag,repo.find(tag.id()).orElseThrow());
 }
}
