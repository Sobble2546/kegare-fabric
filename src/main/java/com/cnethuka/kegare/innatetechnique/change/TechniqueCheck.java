package com.cnethuka.kegare.innatetechnique.change;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.registry.KegareRegistries;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import com.cnethuka.kegare.platform.net.ICustomConfigurationTask;
import com.cnethuka.kegare.platform.net.IPayloadContext;
import com.cnethuka.kegare.platform.net.RegisterConfigurationTasksEvent;

/**
 * The join check (DESIGN §3.34): a client must have the same innate techniques, with the same addons' changes, as the server. While
 * a player connects, the server asks for the client's description of every technique ({@link #describe}); if it differs from its
 * own, the player is turned away with a message naming what differs.
 */
public final class TechniqueCheck implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(Kegare.id("technique_check").toString());

    /** Server to client: "describe your techniques". */
    public record Ask() implements CustomPacketPayload {
        public static final Type<Ask> TYPE = new Type<>(Kegare.id("technique_check_ask"));
        public static final StreamCodec<ByteBuf, Ask> STREAM_CODEC = StreamCodec.unit(new Ask());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Ask payload, IPayloadContext context) {
            context.reply(new Answer(describe()));
        }
    }

    /** Client to server: the client's description of its techniques. */
    public record Answer(List<String> lines) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = new Type<>(Kegare.id("technique_check_answer"));
        public static final StreamCodec<ByteBuf, Answer> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096)).map(Answer::new, Answer::lines);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Answer payload, IPayloadContext context) {
            Optional<Component> differs = difference(describe(), payload.lines());
            if (differs.isPresent()) context.disconnect(differs.get());
            else context.finishCurrentTask(TechniqueCheck.TYPE);
        }
    }

    public static void register(RegisterConfigurationTasksEvent event) {
        event.register(new TechniqueCheck());
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new Ask());
    }

    @Override
    public Type type() {
        return TYPE;
    }

    /**
     * One line per technique (its abilities in order, each with its special, then its ultimate) and one per changed ability (who
     * replaced or tweaked it), sorted: the same on both sides exactly when their techniques and changes are the same.
     */
    public static List<String> describe() {
        List<String> out = new ArrayList<>();
        List<InnateTechnique> techniques = KegareRegistries.INNATE_TECHNIQUE.stream().sorted(Comparator.comparing(t -> t.id().toString())).toList();
        for (InnateTechnique technique : techniques) {
            StringBuilder line = new StringBuilder("technique ").append(technique.id()).append(':');
            for (Ability a : technique.abilities()) line.append(' ').append(withSpecial(a));
            technique.ultimate().ifPresent(u -> line.append(" | ultimate ").append(withSpecial(u)));
            out.add(line.toString());
        }
        ChangeSet.GLOBAL.changed().stream().sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> out.add("change " + id + ": " + String.join(", ", ChangeSet.GLOBAL.describe(id))));
        return out;
    }

    private static String withSpecial(Ability a) {
        return a.id() + a.special().map(s -> "/" + s.id()).orElse("");
    }

    /** Empty when the two descriptions match; otherwise the message the player is turned away with, naming the first difference. */
    public static Optional<Component> difference(List<String> server, List<String> client) {
        Set<String> theirs = new LinkedHashSet<>(client);
        Set<String> ours = new LinkedHashSet<>(server);
        String first = null;
        for (String line : server) if (!theirs.contains(line)) { first = line; break; }
        if (first == null) for (String line : client) if (!ours.contains(line)) { first = line; break; }
        if (first == null) return Optional.empty();
        return Optional.of(Component.translatable("disconnect.kegare.techniques_differ", subject(first)));
    }

    /** What a description line is about, as players read it: the technique's or ability's name if this side knows it, else its ID. */
    private static Component subject(String line) {
        String[] words = line.split(" ", 3);
        String id = words.length > 1 ? words[1].replaceAll(":$", "") : line;
        Identifier rl = Identifier.tryParse(id);
        if (rl != null && words[0].equals("technique")) {
            InnateTechnique technique = KegareRegistries.INNATE_TECHNIQUE.get(rl);
            if (technique != null) return technique.displayName();
        }
        if (rl != null && words[0].equals("change")) {
            for (InnateTechnique technique : KegareRegistries.INNATE_TECHNIQUE) {
                Optional<Ability> ability = technique.findAbility(rl);
                if (ability.isPresent()) return ability.get().displayName();
            }
        }
        return Component.literal(id);
    }
}
