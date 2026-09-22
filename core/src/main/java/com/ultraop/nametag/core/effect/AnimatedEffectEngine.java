package com.ultraop.nametag.core.effect;

import com.ultraop.nametag.core.model.AnimatedEffectSettings;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.TagEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class AnimatedEffectEngine {
    public GlitchFrame render(String source, AnimatedEffectSettings settings, long frameIndex, long seed, IntFunction<Integer> baseColorResolver) {
        if (source == null || settings == null || baseColorResolver == null) throw new IllegalArgumentException("Animation arguments cannot be null");
        List<GlitchFrame.Glyph> glyphs = new ArrayList<>(source.length());
        StringBuilder rendered = new StringBuilder(source.length());
        int length = source.length();
        for (int index = 0; index < length; index++) {
            char character = source.charAt(index);
            int rgb = switch (settings.effectId()) {
                case TagEffect.RAINBOW_ID -> rainbowColor(index, length, frameIndex, settings.intensity());
                case TagEffect.PULSE_ID -> pulseColor(safeBase(baseColorResolver.apply(index)), frameIndex, settings.intensity());
                case TagEffect.WAVE_ID, TagEffect.NEON_ID -> waveColor(safeBase(baseColorResolver.apply(index)), index, frameIndex, settings.intensity());
                case TagEffect.BREATH_ID -> pulseColor(safeBase(baseColorResolver.apply(index)), frameIndex, settings.intensity());
                case TagEffect.BLINK_ID -> blinkColor(safeBase(baseColorResolver.apply(index)), frameIndex, settings.intensity());
                case TagEffect.RGB_ID -> rainbowColor(index, length, frameIndex, settings.intensity());
                default -> throw new IllegalArgumentException("Unsupported animated effect: " + settings.effectId());
            };
            rendered.append(character);
            glyphs.add(new GlitchFrame.Glyph(character, rgb));
        }
        return new GlitchFrame(rendered.toString(), glyphs);
    }
    private static int rainbowColor(int index, int length, long frameIndex, int intensity) {
        double position = length <= 1 ? 0.0 : (double) index / (length - 1);
        double hue = (frameIndex * 0.035 + position * 0.85) % 1.0;
        return blendRgb(0xFFFFFF, hsvToRgb(hue, 1.0, 1.0), intensity / 100.0);
    }
    private static int pulseColor(int base, long frameIndex, int intensity) {
        double amplitude = 0.75 * intensity / 100.0;
        return scaleRgb(base, 1.0 - amplitude * (0.5 + 0.5 * Math.sin(frameIndex * 0.20)));
    }
    private static int blinkColor(int base, long frameIndex, int intensity) {
        return (frameIndex / 3) % 2 == 0 ? base : scaleRgb(base, Math.max(0.05, 1.0 - intensity / 100.0));
    }
    private static int waveColor(int base, int index, long frameIndex, int intensity) {
        double amplitude = 0.75 * intensity / 100.0;
        double phase = frameIndex * 0.35 - index * 0.80;
        return scaleRgb(base, 1.0 - amplitude * (0.5 + 0.5 * Math.sin(phase)));
    }
    private static int blendRgb(int from, int to, double amount) {
        int r=(int)Math.round(((from>>>16)&255)+(((to>>>16)&255)-((from>>>16)&255))*amount);
        int g=(int)Math.round(((from>>>8)&255)+(((to>>>8)&255)-((from>>>8)&255))*amount);
        int b=(int)Math.round((from&255)+((to&255)-(from&255))*amount);
        return r<<16|g<<8|b;
    }
    private static int scaleRgb(int rgb,double factor) {
        int r=(int)Math.round(((rgb>>>16)&255)*factor);
        int g=(int)Math.round(((rgb>>>8)&255)*factor);
        int b=(int)Math.round((rgb&255)*factor);
        return r<<16|g<<8|b;
    }
    private static int safeBase(Integer rgb){return rgb==null?0xFFFFFF:rgb&0xFFFFFF;}
    private static int hsvToRgb(double hue,double saturation,double value) {
        double h=(hue-Math.floor(hue))*6.0; int sector=(int)Math.floor(h); double qf=h-sector;
        double p=value*(1-saturation),q=value*(1-saturation*qf),t=value*(1-saturation*(1-qf));
        double rr,gg,bb;
        switch(sector%6){case 0->{rr=value;gg=t;bb=p;}case 1->{rr=q;gg=value;bb=p;}case 2->{rr=p;gg=value;bb=t;}case 3->{rr=p;gg=q;bb=value;}case 4->{rr=t;gg=p;bb=value;}default->{rr=value;gg=p;bb=q;}}
        return ((int)Math.round(rr*255)<<16)|((int)Math.round(gg*255)<<8)|(int)Math.round(bb*255);
    }
}
