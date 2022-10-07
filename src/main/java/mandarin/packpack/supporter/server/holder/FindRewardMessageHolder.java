package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindRewardMessageHolder extends SearchHolder {
    private final List<Integer> rewards;
    private final String keyword;

    private final double chance;
    private final int amount;

    private final boolean isExtra;
    private final boolean isCompact;
    private final boolean isFrame;

    public FindRewardMessageHolder(@NotNull Message msg, @NotNull Message author, @NotNull String channelID, List<Integer> rewards, String keyword, double chance, int amount, boolean isExtra, boolean isCompact, boolean isFrame, int lang) {
        super(msg, author, channelID, lang);

        this.rewards = rewards;
        this.keyword = keyword;

        this.chance = chance;
        this.amount = amount;

        this.isExtra = isExtra;
        this.isCompact = isCompact;
        this.isFrame = isFrame;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if(i >= rewards.size())
                break;

            String rname = Data.trio(rewards.get(i)) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String name = MultiLangCont.getStatic().RWNAME.getCont(rewards.get(i));

            CommonStatic.getConfig().lang = oldConfig;

            if(name != null && !name.isBlank()) {
                rname += name;
            }

            data.add(rname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Guild g = event.getGuild();
        Message author = getAuthorMessage();

        if(g == null || author == null)
            return;

        try {
            int id = parseDataToInt(event);

            msg.delete().queue();

            List<Stage> stages = EntityFilter.findStageByReward(rewards.get(id), chance, amount);

            if(stages.isEmpty()) {
                ch.sendMessage(LangID.getStringByID("freward_nosta", lang).replace("_", keyword)).queue();
            } else if(stages.size() == 1) {
                Message result = EntityHandler.showStageEmb(stages.get(0), ch, isFrame, isExtra, isCompact, 0, lang);

                if(result != null) {
                    if(StaticStore.timeLimit.containsKey(author.getAuthor().getId())) {
                        StaticStore.timeLimit.get(author.getAuthor().getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(author.getAuthor().getId(), memberLimit);
                    }

                    if(StaticStore.idHolder.containsKey(g.getId())) {
                        StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stages.get(0), author, result, channelID));
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("freward_several", lang).replace("_", keyword)).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > PAGE_CHUNK) {
                    int totalPage = stages.size() / PAGE_CHUNK;

                    if(stages.size() % PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = Command.registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), stages.size(), accumulateStage(stages, false), lang).complete();

                if(res != null) {
                    StaticStore.putHolder(author.getAuthor().getId(), new StageInfoMessageHolder(stages, author, res, ch.getId(), 0, isFrame, isExtra, isCompact, lang));
                }
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FindRewardMessageHolder::onSelected - Failed to perform interaction");
        }
    }

    @Override
    public int getDataSize() {
        return rewards.size();
    }

    private List<String> accumulateStage(List<Stage> stage, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stage.size())
                break;

            Stage st = stage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
                if(mc != null) {
                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String mcn = MultiLangCont.get(mc);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String stmn = MultiLangCont.get(stm);

            CommonStatic.getConfig().lang = oldConfig;

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            CommonStatic.getConfig().lang = lang;

            String stn = MultiLangCont.get(st);

            CommonStatic.getConfig().lang = oldConfig;

            if(st.id != null) {
                if(stn == null || stn.isBlank())
                    stn = Data.trio(st.id.id);
            } else {
                if(stn == null || stn.isBlank())
                    stn = "Unknown";
            }

            name += stn;

            data.add(name);
        }

        return data;
    }
}
