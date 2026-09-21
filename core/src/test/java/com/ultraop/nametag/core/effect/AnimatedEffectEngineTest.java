package com.ultraop.nametag.core.effect;
import com.ultraop.nametag.core.model.AnimatedEffectSettings;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.TagEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedEffectEngineTest {
 @Test void rainbowIsDeterministicAndPreservesText(){var s=new AnimatedEffectSettings(TagEffect.RAINBOW_ID,100,80);var a=new AnimatedEffectEngine().render("OWNER",s,0,1L,i->0xFFFFFF);var b=new AnimatedEffectEngine().render("OWNER",s,0,1L,i->0xFFFFFF);assertEquals("OWNER",a.text());assertEquals(a,b);}
 @Test void rainbowChangesAcrossFrames(){var e=new AnimatedEffectEngine();var s=new AnimatedEffectSettings(TagEffect.RAINBOW_ID,100,80);assertNotEquals(e.render("OWNER",s,0,1L,i->0xFFFFFF),e.render("OWNER",s,1,1L,i->0xFFFFFF));}
 @Test void pulseAndWaveRespectBaseAtZeroIntensity(){var e=new AnimatedEffectEngine();assertTrue(e.render("OWNER",new AnimatedEffectSettings(TagEffect.PULSE_ID,0,80),7,1L,i->0x336699).glyphs().stream().allMatch(g->g.rgb()==0x336699));assertTrue(e.render("OWNER",new AnimatedEffectSettings(TagEffect.WAVE_ID,0,80),7,1L,i->0x336699).glyphs().stream().allMatch(g->g.rgb()==0x336699));}
 @Test void unsupportedEffectRejected(){assertThrows(IllegalArgumentException.class,()->new AnimatedEffectSettings(TagEffect.GLITCH_ID,45,80));}
}
