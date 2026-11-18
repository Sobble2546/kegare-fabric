package com.cnethuka.kegare.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 26.x has no immediate buffer source ({@code MultiBufferSource} is gone): world geometry is handed to the frame's
 * {@link SubmitNodeCollector} and drawn later. The shared effect code still writes vertices into "a buffer for a render type" and
 * ends the batch; here that records the vertices and, at the end of the batch, submits them as one custom-geometry node. The 26.x
 * source renames point {@code MultiBufferSource} at this class (targets/26.renames).
 *
 * <p>Only valid while {@link LevelStages} is posting stages (the collector is bound for that time). A batch ended at any other time
 * is dropped. UNVERIFIED in game.
 */
public interface KBuffers {
    VertexConsumer getBuffer(RenderType type);

    static BufferSource immediate() {
        return BufferSource.INSTANCE;
    }

    final class BufferSource implements KBuffers {
        static final BufferSource INSTANCE = new BufferSource();

        @Nullable
        private SubmitNodeCollector collector;
        private final Map<RenderType, Recording> open = new LinkedHashMap<>();

        private BufferSource() {}

        static void bind(@Nullable SubmitNodeCollector collector) {
            INSTANCE.endBatch();
            INSTANCE.collector = collector;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return open.computeIfAbsent(type, t -> new Recording());
        }

        public void endBatch(RenderType type) {
            Recording recording = open.remove(type);
            if (recording != null) submit(type, recording);
        }

        public void endBatch() {
            if (open.isEmpty()) return;
            Map<RenderType, Recording> all = new LinkedHashMap<>(open);
            open.clear();
            all.forEach(this::submit);
        }

        public void endLastBatch() {
            endBatch();
        }

        private void submit(RenderType type, Recording recording) {
            if (collector == null || recording.size == 0) return;
            // The vertices already carry the pose the effect code applied; the stack handed over is the camera-relative identity.
            collector.submitCustomGeometry(new PoseStack(), type, (pose, out) -> recording.replay(out));
        }
    }

    /** Vertex calls kept as ints (floats by their raw bits), four per call: the call, then up to three arguments. */
    final class Recording implements VertexConsumer {
        private static final int VERTEX = 0, COLOR = 1, UV = 2, UV1 = 3, UV2 = 4, UV3 = 5, NORMAL = 6, LINE_WIDTH = 7;
        private int[] data = new int[256];
        int size;

        private VertexConsumer put(int call, int a, int b, int c) {
            if (size + 4 > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = call;
            data[size++] = a;
            data[size++] = b;
            data[size++] = c;
            return this;
        }

        private static int bits(float value) {
            return Float.floatToRawIntBits(value);
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { return put(VERTEX, bits(x), bits(y), bits(z)); }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return put(COLOR, (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF, 0, 0); }
        @Override public VertexConsumer setColor(int argb) { return put(COLOR, argb, 0, 0); }
        @Override public VertexConsumer setUv(float u, float v) { return put(UV, bits(u), bits(v), 0); }
        @Override public VertexConsumer setUv1(int u, int v) { return put(UV1, u, v, 0); }
        @Override public VertexConsumer setUv2(int u, int v) { return put(UV2, u, v, 0); }
        @Override public VertexConsumer setUv3(float u, float v) { return put(UV3, bits(u), bits(v), 0); }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return put(NORMAL, bits(x), bits(y), bits(z)); }
        @Override public VertexConsumer setLineWidth(float width) { return put(LINE_WIDTH, bits(width), 0, 0); }

        void replay(VertexConsumer out) {
            for (int i = 0; i < size; i += 4) {
                int a = data[i + 1], b = data[i + 2], c = data[i + 3];
                switch (data[i]) {
                    case VERTEX -> out.addVertex(Float.intBitsToFloat(a), Float.intBitsToFloat(b), Float.intBitsToFloat(c));
                    case COLOR -> out.setColor(a);
                    case UV -> out.setUv(Float.intBitsToFloat(a), Float.intBitsToFloat(b));
                    case UV1 -> out.setUv1(a, b);
                    case UV2 -> out.setUv2(a, b);
                    case UV3 -> out.setUv3(Float.intBitsToFloat(a), Float.intBitsToFloat(b));
                    case NORMAL -> out.setNormal(Float.intBitsToFloat(a), Float.intBitsToFloat(b), Float.intBitsToFloat(c));
                    default -> out.setLineWidth(Float.intBitsToFloat(a));
                }
            }
        }
    }
}
