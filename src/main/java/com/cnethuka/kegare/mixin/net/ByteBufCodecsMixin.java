package com.cnethuka.kegare.mixin.net;

import com.cnethuka.kegare.platform.net.IdRemapCodecs;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wraps vanilla's two registry codec factories (DESIGN §3.5, "Via servers"). {@code registry} is the one entity types and items are read and
 * written with; {@code holder} is the sound event's. Both are static methods of an interface, so this is an interface mixin and its
 * handlers are private static. The injections are optional ({@code require = 0}): if one does not apply, only the Via bridge is lost,
 * and {@code IdRemap} says so when a table needs it.
 */
@Mixin(ByteBufCodecs.class)
public interface ByteBufCodecsMixin {
    @Inject(method = "registry(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static <T, R> void kegare$registry(ResourceKey<? extends Registry<T>> key, Function<Registry<T>, IdMap<R>> extractor,
                                               CallbackInfoReturnable<StreamCodec<RegistryFriendlyByteBuf, R>> cir) {
        cir.setReturnValue(IdRemapCodecs.registry(key, extractor, cir.getReturnValue()));
    }

    @Inject(method = "holder(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/codec/StreamCodec;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static <T> void kegare$holder(ResourceKey<? extends Registry<T>> key, StreamCodec<? super RegistryFriendlyByteBuf, T> direct,
                                          CallbackInfoReturnable<StreamCodec<RegistryFriendlyByteBuf, Holder<T>>> cir) {
        cir.setReturnValue(IdRemapCodecs.holder(key, direct, cir.getReturnValue()));
    }
}
