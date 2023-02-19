package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class FormStatMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;
    private final ConfigHolder config;

    private final boolean talent;
    private final boolean isFrame;
    private final boolean extra;
    private final boolean compact;
    private final boolean isTrueForm;
    private final Level lv;

    public FormStatMessageHolder(ArrayList<Form> form, Message author, ConfigHolder config, IDHolder holder, Message msg, String channelID, int param, Level lv, int lang) {
        super(msg, author, channelID, lang);

        this.form = form;
        this.config = config;

        this.talent = (param & 2) > 0 || lv.getTalents().length > 0;
        this.isFrame = (param & 4) == 0 && config.useFrame;
        this.extra = (param & 8) > 0 || config.extra;
        this.compact = (param & 16) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        this.isTrueForm = (param & 32) > 0;
        this.lv = lv;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(f) != null)
                fname += MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        msg.delete().queue();

        try {
            Form f = form.get(id);

            Message result = EntityHandler.showUnitEmb(f, ch, getAuthorMessage(), config, isFrame, talent, extra, isTrueForm, f.fid == 2, lv, lang, true, compact);

            if(result != null) {
                User u = event.getUser();

                StaticStore.removeHolder(u.getId(), FormStatMessageHolder.this);

                StaticStore.putHolder(u.getId(), new FormButtonHolder(form.get(id), getAuthorMessage(), result, config, isFrame, talent, extra, compact, lv, lang, channelID));
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormStatMessageHolder::onSelected - Failed to perform showing unit embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
