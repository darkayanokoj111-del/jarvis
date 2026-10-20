package com.jarvis.assistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

public class OrbView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float phase = 0f;
    private float level = 1f;

    public OrbView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(5f);
        ring.setColor(0xFF38E8FF);
        glow.setColor(0x2238E8FF);
    }

    public void setLevel(float l) { level = l; }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2, cy = h / 2;
        phase += 0.08f;
        float pulse = 1f + 0.1f * level * (float) Math.sin(phase * 3);
        float r = Math.min(w, h) * 0.2f * pulse;

        for (int i = 4; i >= 1; i--) {
            c.drawCircle(cx, cy, r * (1f + 0.35f * i), glow);
        }
        p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.2f,
                new int[]{0xFFFFFFFF, 0xFF38E8FF, 0xFF0A6CFF, 0xFF001A4D},
                new float[]{0f, 0.25f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);

        float r1 = r * 1.9f;
        box.set(cx - r1, cy - r1, cx + r1, cy + r1);
        c.drawArc(box, phase * 50f, 110f, false, ring);
        c.drawArc(box, phase * 50f + 180f, 110f, false, ring);
        float r2 = r * 2.4f;
        box.set(cx - r2, cy - r2, cx + r2, cy + r2);
        c.drawArc(box, -phase * 35f, 70f, false, ring);

        postInvalidateOnAnimation();
    }
}
