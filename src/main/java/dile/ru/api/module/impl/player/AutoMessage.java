package dile.ru.api.module.impl.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.AttackEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.frageffect.FragEffectDeathMemoryTracker;
import dile.ru.api.settings.impl.BooleanSetting;

import java.util.Random;

public final class AutoMessage extends Module {
    private static final String[] MESSAGES = {
            "!%s, тебя не я щас трахнул, тя трахнул создатель Dile Client - bfbddb5f",
            "!раком тя поставил, %s как твою родословную by Dile Client",
            "!%s, дфд (bfbddb5f) создал этот Dile Client 1.21.11 и трахнул тебя в 0(",
            "!%s, ez by Dile Client 1.21.11 + dfd",
            "!не ты фрик рял, качни Dile Client 1.21.11, %s, а то здох",
            "!есче, %s, тя убил не я а bfbddb5f (создать Dile Client)",
            "!фри килл для бесплатного чита Dile Client 1.21.11, %s otpor",
            "!%s, бичня, всосала фри читу Dile Client 1.21.11 by bfbddb5f",
            "!бля, %s, братиш, ты думаешь рял тебя я убил? тебя убил bfbddb5f и Dile Client 1.21.11",
            "!я тя по экрану жирного %s с помощью фри чита Dile Client 1.21.11 размазал фрик ебаный)",
            "!чета отлетел, %s ведь не юзал лучший фри чит Dile Client 1.21.11",
            "!%s, качай Dile Client 1.21.11 бичка"
    };

    private static final String[] DEATH_MESSAGES = {
            "!Балин, мышка сломалась",
            "!ой, отлетел от лага, зато Dile Client 1.21.11 качаю",
            "!не по фэншую умер, зато на фри чите Dile Client 1.21.11 от bfbddb5f",
            "!первый заход умирал, потом Dile Client 1.21.11 сделанный bfbddb5f выносит всех",
            "!грузится Dile Client 1.21.11, лучшие его модули, поэтому я и отлетел",
            "!упал как мешок картошки, зато на фри диле от bfbddb5f",
            "!кто-то просто не юзал Dile Client 1.21.11, вот я и пожалел его.",
            "!бывает, раунд за Dile Client 1.21.11, заберите фри чит",
            "!сегодня я фрик, завтра ты, Dile Client 1.21.11 меняет роли",
            "!мать свою проиграл в камень ножницы бумага, а Dile Client 1.21.11 всё равно качаю",
            "!мне бы Dile Client 1.21.11 от bfbddb5f до конца доесть, а меня тут убили",
            "!клавиатура зависла, а ведь Dile Client 1.21.11 сам всех выносит",
            "!пальцы не на тех кнопках были, Dile Client 1.21.11 бы не подвёл",
            "!отключил Dile Client 1.21.11 на 5 секунд и сразу умер, читы не выключаются",
            "!bfbddb5f скажет что это недостойно игрока Dile Client 1.21.11",
            "!перезагружаю Dile Client 1.21.11, фрики не расходитесь",
            "!мой тормозной интернет против Dile Client 1.21.11 от bfbddb5f проиграл",
            "!калькулятор за шмот не принял, Dile Client 1.21.11 хотя бы шмот не съел",
            "!кто меня убил тот теперь должник Dile Client 1.21.11 и bfbddb5f",
            "!респект тому кто убил, а теперь качайте Dile Client 1.21.11 by bfbddb5f",
            "!умер как бот без Dile Client 1.21.11, скачал и не умирал, навечно в истории",
            "!монитор потёк, мышка сломалась, Dile Client 1.21.11 работает",
            "!не ошибается тот кто юзает Dile Client 1.21.11, а я ошибся",
            "!атака фриков удалась, но Dile Client 1.21.11 всё равно топ",
            "!вырубился от красоты Dile Client 1.21.11, убили пока залипал на красоту клиента",
            "!bfbddb5f за такое отключит мне Dile Client 1.21.11, не позорьте меня",
            "!зато я на фри чите Dile Client 1.21.11, а ты платный бот",
            "!умер чисто из вежливости, Dile Client 1.21.11 бы так не поступил",
            "!лол, и это меня убило? Dile Client 1.21.11 от bfbddb5f возродит меня сильнее",
            "!не грустите, Dile Client 1.21.11 already downloaded by bfbddb5f"
    };

    private final BooleanSetting deathMessage = register(new BooleanSetting("При смерти", "Отправлять сообщение в глобальный чат при собственной смерти.", false));

    private final FragEffectDeathMemoryTracker deathTracker = new FragEffectDeathMemoryTracker();
    private final Random random = new Random();
    private boolean wasDead;

    public AutoMessage() {
        super("Auto Message", "Sends a message when you kill an aura target.", ModuleCategory.PLAYER);
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        LivingEntity auraTarget = AuraModule.target;
        if (auraTarget != null && auraTarget.isAlive() && auraTarget.getHealth() > 0.0F) {
            deathTracker.remember(auraTarget, true);
        }
        deathTracker.tick(this::onTargetKilled);
        handlePlayerDeath();
    }

    private void handlePlayerDeath() {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        boolean dead = mc.player.isDeadOrDying() || mc.player.deathTime > 0;
        if (dead) {
            if (!wasDead && deathMessage.getValue()) {
                wasDead = true;
                mc.player.connection.sendChat(DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)]);
            }
        } else {
            wasDead = false;
        }
    }

    @SubscribeEvent
    private void onAttack(AttackEvent event) {
        Entity entity = event.getTarget();
        if (entity instanceof LivingEntity living && living.isAlive() && living.getHealth() > 0.0F) {
            deathTracker.remember(living, true);
        }
    }

    private void onTargetKilled(LivingEntity entity) {
        if (mc.player == null || mc.getConnection() == null) return;

        String name = entity.getName().getString();
        String template = MESSAGES[random.nextInt(MESSAGES.length)];
        String message = String.format(template, name);
        mc.player.connection.sendChat(message);
    }

    @Override
    protected void onEnable() {
        wasDead = mc.player != null && (mc.player.isDeadOrDying() || mc.player.deathTime > 0);
    }

    @Override
    protected void onDisable() {
        deathTracker.clear();
        wasDead = false;
    }
}
