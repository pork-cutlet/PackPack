package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.unit.Form;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.FormSpriteHolder;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.ArrayList;

public class FormSprite extends TimedConstraintCommand {
    private static final int PARAM_UNI = 2;
    private static final int PARAM_UDI = 4;
    private static final int PARAM_EDI = 8;

    public FormSprite(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getMessage(event).split(" ");

        if(contents.length == 1) {
            ch.createMessage(LangID.getStringByID("fimg_more", lang)).subscribe();
        } else {
            String search = filterCommand(getMessage(event));

            if(search.isBlank()) {
                ch.createMessage(LangID.getStringByID("fimg_more", lang)).subscribe();
                return;
            }

            ArrayList<Form> forms = EntityFilter.findUnitWithName(search);

            if(forms.isEmpty()) {
                ch.createMessage(LangID.getStringByID("formst_nounit", lang).replace("_", filterCommand(getMessage(event)))).subscribe();
                disableTimer();
            } else if(forms.size() == 1) {
                int param = checkParameter(getMessage(event));

                EntityHandler.getFormSprite(forms.get(0), ch, getModeFromParam(param), lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getMessage(event))));

                String check;

                if(forms.size() <= 20)
                    check = "";
                else
                    check = LangID.getStringByID("formst_next", lang);

                sb.append("```md\n").append(check);

                for(int i = 0; i < 20; i++) {
                    if(i >= forms.size())
                        break;

                    Form f = forms.get(i);

                    String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

                    String name = StaticStore.safeMultiLangGet(f, lang);

                    if(name != null)
                        fname += name;

                    sb.append(i+1).append(". ").append(fname).append("\n");
                }

                if(forms.size() > 20)
                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(forms.size()/20 + 1)));

                sb.append(LangID.getStringByID("formst_can", lang));
                sb.append("```");

                int param = checkParameter(getMessage(event));

                int mode = getModeFromParam(param);

                Message res = ch.createMessage(sb.toString()).block();

                if(res != null) {
                    event.getMember().ifPresent(member -> StaticStore.putHolder(member.getId().asString(), new FormSpriteHolder(forms, event.getMessage(), res, ch.getId().asString(), mode, lang)));
                }

                disableTimer();
            }
        }
    }

    private int checkParameter(String message) {
        String[] contents = message.split(" ");

        int res = 1;

        label:
        for (String content : contents) {
            switch (content) {
                case "-uni":
                    res |= PARAM_UNI;
                    break label;
                case "-udi":
                    res |= PARAM_UDI;
                    break label;
                case "-edi":
                    res |= PARAM_EDI;
                    break label;
            }
        }

        return res;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean preParamEnd = false;

        boolean uni = false;
        boolean udi = false;
        boolean edi = false;

        for(int i = 1; i < contents.length; i++) {
            if(!preParamEnd) {
                switch (contents[i]) {
                    case "-uni":
                        if (!uni) {
                            uni = true;
                        } else {
                            i--;
                            preParamEnd = true;
                        }
                        break;
                    case "-udi":
                        if(!udi) {
                            udi = true;
                        } else {
                            i--;
                            preParamEnd = true;
                        }
                        break;
                    case "-edi":
                        if(!edi) {
                            edi = true;
                        } else {
                            i--;
                            preParamEnd = true;
                        }
                        break;
                    default:
                        i--;
                        preParamEnd = true;
                        break;
                }
            } else {
                result.append(contents[i]);

                if(i < contents.length - 1)
                    result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private int getModeFromParam(int param) {
        if((param & PARAM_UNI) > 0)
            return 1;
        if((param & PARAM_UDI) > 0)
            return 2;
        if((param & PARAM_EDI) > 0)
            return 3;
        else
            return 0;
    }
}