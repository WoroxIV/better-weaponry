package com.betterweaponry.tutorial;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une scène de tutoriel : des personnages, ce qu'ils font à chaque instant, ce qui s'affiche autour d'eux, et les textes
 * qui décrivent l'action. Cette classe ne fait que décrire et calculer ; le dessin est dans {@code client.tutorial}.
 * Le temps est compté en ticks (20 par seconde).
 */
public final class TutorialScene {
    /** Durée d'un coup de bras. */
    public static final int SWING_TICKS = 8;
    /** Durée du flash rouge et du petit recul d'un personnage touché. */
    public static final int HURT_TICKS = 10;
    public static final int SPARK_TICKS = 10;
    private static final int FADE_TICKS = 4;
    private static final int TURN_TICKS = 6;
    private static final float WALK_PHASE_PER_TICK = 0.7F;
    private static final double HURT_RECOIL = 0.25D;

    public enum ActorKind { PLAYER, ZOMBIE }

    public enum Hand { MAIN, OFF, BOTH }

    public record Step(int start, Component caption) {
    }

    /** Disque ou secteur au sol, centré sur un personnage (ou un point fixe) et orienté comme lui. */
    public record Zone(@Nullable String actor, double x, double z, double radius, double angle, int color, int from, int to) {
    }

    /** Trait au sol entre deux personnages, ou entre un personnage et un point fixe. */
    public record Beam(@Nullable String fromActor, double fromX, double fromZ, @Nullable String toActor, double toX, double toZ, int color, int from, int to) {
    }

    /** Texte flottant au-dessus d'un personnage ; {@code rise} le fait monter (dégâts). */
    public record Label(String actor, double x, double y, double z, Component text, int color, int from, int to, boolean rise) {
    }

    public record Spark(String actor, int tick, int color) {
    }

    /**
     * Frise d'une tentative de parade : garde levée, fenêtre de parade parfaite, instant du coup.
     *
     * @param guard     tick où la garde se lève
     * @param windowEnd dernier tick de la fenêtre de parade parfaite
     * @param hit       tick du coup
     */
    public record TimingBar(int from, int to, int guard, int windowEnd, int hit) {
        /** Le coup tombe-t-il dans la fenêtre ? */
        public boolean perfect() {
            return hit >= guard && hit < windowEnd;
        }
    }

    /** État d'un personnage à un instant donné. */
    public record ActorPose(double x, double z, float yaw, float walkPhase, float walkAmount, float mainSwing, float offSwing,
                            boolean guardMain, boolean guardOff, float hurt, ItemStack mainHand, ItemStack offHand) {
    }

    private record Move(int from, int to, double x, double z, boolean dash) {
    }

    private record Turn(int tick, float yaw) {
    }

    private record Swing(int tick, Hand hand) {
    }

    private record Guard(int from, int to, Hand hand) {
    }

    private record Hurt(int tick, @Nullable String source) {
    }

    private record Equip(int tick, boolean offHand, ItemStack stack) {
    }

    /** Un personnage de la scène et son emploi du temps. */
    public static final class Actor {
        private final String id;
        private final ActorKind kind;
        private final double startX;
        private final double startZ;
        private final float startYaw;
        private final ItemStack startMain;
        private final ItemStack startOff;
        private boolean armored;
        private final List<Move> moves = new ArrayList<>();
        private final List<Turn> turns = new ArrayList<>();
        private final List<Swing> swings = new ArrayList<>();
        private final List<Guard> guards = new ArrayList<>();
        private final List<Hurt> hurts = new ArrayList<>();
        private final List<Equip> equips = new ArrayList<>();

        private Actor(String id, ActorKind kind, double x, double z, float yaw, ItemStack main, ItemStack off) {
            this.id = id;
            this.kind = kind;
            this.startX = x;
            this.startZ = z;
            this.startYaw = yaw;
            this.startMain = main;
            this.startOff = off;
        }

        public String id() {
            return id;
        }

        public ActorKind kind() {
            return kind;
        }

        public boolean armored() {
            return armored;
        }

        /** Position seule : sert aussi à calculer le recul, sans dépendre des autres personnages. */
        private double[] positionAt(float tick) {
            double x = startX;
            double z = startZ;
            for (Move move : moves) {
                if (tick >= move.to) {
                    x = move.x;
                    z = move.z;
                } else if (tick > move.from) {
                    float progress = (tick - move.from) / (move.to - move.from);
                    float eased = move.dash ? 1.0F - (1.0F - progress) * (1.0F - progress) : progress;
                    return new double[]{Mth.lerp(eased, x, move.x), Mth.lerp(eased, z, move.z), move.dash ? 0.0D : 1.0D};
                }
            }
            return new double[]{x, z, 0.0D};
        }
    }

    private final String id;
    private final int duration;
    private final Map<String, Actor> actors;
    private final List<Step> steps;
    private final List<Zone> zones;
    private final List<Beam> beams;
    private final List<Label> labels;
    private final List<Spark> sparks;
    private final List<TimingBar> bars;

    private TutorialScene(Builder builder) {
        this.id = builder.id;
        this.duration = builder.duration;
        this.actors = builder.actors;
        this.steps = List.copyOf(builder.steps);
        this.zones = List.copyOf(builder.zones);
        this.beams = List.copyOf(builder.beams);
        this.labels = List.copyOf(builder.labels);
        this.sparks = List.copyOf(builder.sparks);
        this.bars = List.copyOf(builder.bars);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public int duration() {
        return duration;
    }

    public List<Step> steps() {
        return steps;
    }

    public List<Actor> actors() {
        return List.copyOf(actors.values());
    }

    public Actor actor(String actorId) {
        return actors.get(actorId);
    }

    public List<Zone> zones() {
        return zones;
    }

    public List<Beam> beams() {
        return beams;
    }

    public List<Label> labels() {
        return labels;
    }

    public List<Spark> sparks() {
        return sparks;
    }

    public List<TimingBar> bars() {
        return bars;
    }

    /** Indice de l'étape en cours à cet instant. */
    public int stepAt(float tick) {
        int index = 0;
        for (int i = 0; i < steps.size(); i++) {
            if (tick >= steps.get(i).start()) index = i;
        }
        return index;
    }

    /** Opacité d'un élément visible de {@code from} à {@code to}, avec un court fondu au début et à la fin. */
    public static float fade(float tick, int from, int to) {
        if (tick < from || tick >= to) return 0.0F;
        return Math.min(1.0F, Math.min((tick - from) / FADE_TICKS, (to - tick) / FADE_TICKS));
    }

    /** Où et dans quel état se trouve ce personnage à cet instant. */
    public ActorPose pose(Actor actor, float tick) {
        double[] position = actor.positionAt(tick);
        double x = position[0];
        double z = position[1];
        float walkAmount = (float) position[2];

        float hurt = 0.0F;
        for (Hurt event : actor.hurts) {
            if (tick < event.tick || tick >= event.tick + HURT_TICKS) continue;
            float progress = (tick - event.tick) / HURT_TICKS;
            hurt = 1.0F - progress;
            if (event.source != null) {
                double[] source = actors.get(event.source).positionAt(event.tick);
                double dx = x - source[0];
                double dz = z - source[1];
                double length = Math.max(1.0E-3D, Math.sqrt(dx * dx + dz * dz));
                double recoil = HURT_RECOIL * Mth.sin(progress * Mth.PI);
                x += dx / length * recoil;
                z += dz / length * recoil;
            }
        }

        float yaw = actor.startYaw;
        for (Turn turn : actor.turns) {
            if (tick >= turn.tick + TURN_TICKS) {
                yaw = turn.yaw;
            } else if (tick > turn.tick) {
                yaw = Mth.rotLerp((tick - turn.tick) / TURN_TICKS, yaw, turn.yaw);
            }
        }

        float mainSwing = 0.0F;
        float offSwing = 0.0F;
        for (Swing swing : actor.swings) {
            if (tick < swing.tick || tick >= swing.tick + SWING_TICKS) continue;
            float progress = (tick - swing.tick) / SWING_TICKS;
            if (swing.hand != Hand.OFF) mainSwing = progress;
            if (swing.hand != Hand.MAIN) offSwing = progress;
        }

        boolean guardMain = false;
        boolean guardOff = false;
        for (Guard guard : actor.guards) {
            if (tick < guard.from || tick >= guard.to) continue;
            guardMain |= guard.hand != Hand.OFF;
            guardOff |= guard.hand != Hand.MAIN;
        }

        ItemStack main = actor.startMain;
        ItemStack off = actor.startOff;
        for (Equip equip : actor.equips) {
            if (tick < equip.tick) continue;
            if (equip.offHand) off = equip.stack;
            else main = equip.stack;
        }
        return new ActorPose(x, z, yaw, tick * WALK_PHASE_PER_TICK, walkAmount, mainSwing, offSwing, guardMain, guardOff, hurt, main, off);
    }

    /** Écriture des scènes, dans {@link TutorialScenes}. */
    public static final class Builder {
        private final String id;
        private final Map<String, Actor> actors = new LinkedHashMap<>();
        private final List<Step> steps = new ArrayList<>();
        private final List<Zone> zones = new ArrayList<>();
        private final List<Beam> beams = new ArrayList<>();
        private final List<Label> labels = new ArrayList<>();
        private final List<Spark> sparks = new ArrayList<>();
        private final List<TimingBar> bars = new ArrayList<>();
        private int duration;

        private Builder(String id) {
            this.id = id;
        }

        public Builder actor(String actorId, ActorKind kind, double x, double z, float yaw, ItemStack main, ItemStack off) {
            actors.put(actorId, new Actor(actorId, kind, x, z, yaw, main, off));
            return this;
        }

        public Builder armored(String actorId) {
            actors.get(actorId).armored = true;
            return this;
        }

        public Builder step(int start, Component caption) {
            steps.add(new Step(start, caption));
            return this;
        }

        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        public Builder walk(String actorId, int from, int to, double x, double z) {
            actors.get(actorId).moves.add(new Move(from, to, x, z, false));
            return this;
        }

        public Builder dash(String actorId, int from, int to, double x, double z) {
            actors.get(actorId).moves.add(new Move(from, to, x, z, true));
            return this;
        }

        public Builder turn(String actorId, int tick, float yaw) {
            actors.get(actorId).turns.add(new Turn(tick, yaw));
            return this;
        }

        public Builder swing(String actorId, int tick, Hand hand) {
            actors.get(actorId).swings.add(new Swing(tick, hand));
            return this;
        }

        public Builder guard(String actorId, int from, int to, Hand hand) {
            actors.get(actorId).guards.add(new Guard(from, to, hand));
            return this;
        }

        public Builder hurt(String actorId, int tick, @Nullable String source) {
            actors.get(actorId).hurts.add(new Hurt(tick, source));
            return this;
        }

        public Builder equip(String actorId, int tick, boolean offHand, ItemStack stack) {
            actors.get(actorId).equips.add(new Equip(tick, offHand, stack));
            return this;
        }

        /** Disque (angle 360) ou secteur centré sur un personnage. */
        public Builder zone(String actorId, double radius, double angle, int color, int from, int to) {
            zones.add(new Zone(actorId, 0.0D, 0.0D, radius, angle, color, from, to));
            return this;
        }

        public Builder beam(String fromActor, String toActor, int color, int from, int to) {
            beams.add(new Beam(fromActor, 0.0D, 0.0D, toActor, 0.0D, 0.0D, color, from, to));
            return this;
        }

        public Builder trail(double fromX, double fromZ, double toX, double toZ, int color, int from, int to) {
            beams.add(new Beam(null, fromX, fromZ, null, toX, toZ, color, from, to));
            return this;
        }

        /** Texte fixe au-dessus d'un personnage. */
        public Builder label(String actorId, Component text, int color, int from, int to) {
            labels.add(new Label(actorId, 0.0D, TutorialScenes.LABEL_HEIGHT, 0.0D, text, color, from, to, false));
            return this;
        }

        /** Texte placé à côté d'un personnage (ex : près de sa main secondaire). */
        public Builder labelAt(String actorId, double x, double y, double z, Component text, int color, int from, int to) {
            labels.add(new Label(actorId, x, y, z, text, color, from, to, false));
            return this;
        }

        /** Texte qui monte en s'effaçant, pour les dégâts. */
        public Builder riseLabel(String actorId, Component text, int color, int from) {
            labels.add(new Label(actorId, 0.0D, TutorialScenes.RISE_START_HEIGHT, 0.0D, text, color, from, from + TutorialScenes.RISE_TICKS, true));
            return this;
        }

        public Builder spark(String actorId, int tick, int color) {
            sparks.add(new Spark(actorId, tick, color));
            return this;
        }

        public Builder bar(int from, int to, int guard, int windowEnd, int hit) {
            bars.add(new TimingBar(from, to, guard, windowEnd, hit));
            return this;
        }

        public TutorialScene build() {
            return new TutorialScene(this);
        }
    }
}
