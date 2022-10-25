package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.anim.EAnimD;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.ImageDrawing;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.EnemyAnimMessageHolder;
import mandarin.packpack.supporter.server.holder.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnemyImage extends TimedConstraintCommand {
    private static final int PARAM_TRANSPARENT = 2;
    private static final int PARAM_DEBUG = 4;

    public EnemyImage(ConstraintCommand.ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_ENEMYIMAGE_ID);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getContent(event).split(" ");

        if(list.length >= 2) {
            File temp = new File("./temp");

            if(!temp.exists()) {
                boolean res = temp.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder : "+temp.getAbsolutePath());
                    return;
                }
            }

            String search = filterCommand(getContent(event));

            if(search.isBlank()) {
                createMessageWithNoPings(ch, LangID.getStringByID("eimg_more", lang), getMessage(event));

                return;
            }

            ArrayList<Enemy> enemies = EntityFilter.findEnemyWithName(search, lang);

            if(enemies.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("enemyst_noenemy", lang).replace("_", filterCommand(getContent(event))), getMessage(event));

                disableTimer();
            } else if(enemies.size() == 1) {
                int param = checkParameters(getContent(event));
                int mode = getMode(getContent(event));
                int frame = getFrame(getContent(event));

                enemies.get(0).anim.load();

                if(mode >= enemies.get(0).anim.anims.length)
                    mode = 0;

                EAnimD<?> anim = enemies.get(0).getEAnim(ImageDrawing.getAnimType(mode, enemies.get(0).anim.anims.length));

                File img = ImageDrawing.drawAnimImage(anim, frame, 1.0, (param & PARAM_TRANSPARENT) > 0, (param & PARAM_DEBUG) > 0);

                enemies.get(0).anim.unload();

                if(img != null) {

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String fName = MultiLangCont.get(enemies.get(0));

                    CommonStatic.getConfig().lang = oldConfig;

                    if(fName == null || fName.isBlank())
                        fName = enemies.get(0).names.toString();

                    if(fName.isBlank())
                        fName = LangID.getStringByID("data_enemy", lang)+" "+ Data.trio(enemies.get(0).id.id);

                    sendMessageWithFile(
                            ch,
                            LangID.getStringByID("eimg_result", lang).replace("_", fName).replace(":::", getModeName(mode, enemies.get(0).anim.anims.length)).replace("=", String.valueOf(frame)),
                            img,
                            getMessage(event)
                    );
                }
            } else {
                CommonStatic.getConfig().lang = lang;

                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", filterCommand(getContent(event))));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(enemies);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(enemies.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = enemies.size() / SearchHolder.PAGE_CHUNK;

                    if(enemies.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), enemies.size(), data, lang).complete();

                int param = checkParameters(getContent(event));
                int mode = getMode(getContent(event));
                int frame = getFrame(getContent(event));

                if(res != null) {
                    Member member = getMember(event);

                    if(member != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(member.getId(), new EnemyAnimMessageHolder(enemies, msg, res, ch.getId(), mode, frame, ((param & PARAM_TRANSPARENT) > 0), ((param & PARAM_DEBUG) > 0), lang, false, false, false));
                    }
                }

                disableTimer();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("eimg_more", lang)).queue();
            disableTimer();
        }
    }

    private int getMode(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-m") || msg[i].equals("-mode")) {
                if(i < msg.length - 1) {
                    if(LangID.getStringByID("fimg_walk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 0;
                    else if(LangID.getStringByID("fimg_idle", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 1;
                    else if(LangID.getStringByID("fimg_atk", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 2;
                    else if(LangID.getStringByID("fimg_hb", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 3;
                    else if(LangID.getStringByID("fimg_enter", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrdown", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 4;
                    else if(LangID.getStringByID("fimg_burrmove", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 5;
                    else if(LangID.getStringByID("fimg_burrup", lang).toLowerCase(Locale.ENGLISH).contains(msg[i+1].toLowerCase(Locale.ENGLISH)))
                        return 6;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getFrame(String message) {
        String [] msg = message.split(" ");

        for(int i = 0; i < msg.length; i++) {
            if(msg[i].equals("-f") || msg[i].equals("-fr")) {
                if(i < msg.length - 1 && StaticStore.isNumeric(msg[i+1])) {
                    return StaticStore.safeParseInt(msg[i+1]);
                }
            }
        }

        return 0;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].toLowerCase(Locale.ENGLISH).split(" ");

            label:
            for(int i = 0; i < pureMessage.length; i++) {
                switch (pureMessage[i]) {
                    case "-t":
                        if((result & PARAM_TRANSPARENT) == 0) {
                            result |= PARAM_TRANSPARENT;
                        } else {
                            break label;
                        }
                        break;
                    case "-d":
                    case "-debug":
                        if((result & PARAM_DEBUG) == 0) {
                            result |= PARAM_DEBUG;
                        } else {
                            break label;
                        }
                        break;
                    case "-f":
                    case "-fr":
                        if(i < pureMessage.length - 1 && StaticStore.isNumeric(pureMessage[i+1])) {
                            i++;
                        } else {
                            break label;
                        }
                        break;
                    case "-m":
                    case "-mode":
                        if(i < pureMessage.length -1) {
                            i++;
                        } else {
                            break label;
                        }
                }
            }
        }

        return result;
    }

    String filterCommand(String message) {
        String[] contents = message.split(" ");

        if(contents.length == 1)
            return "";

        StringBuilder result = new StringBuilder();

        boolean debug = false;
        boolean trans = false;

        boolean mode = false;
        boolean frame = false;

        for(int i = 1; i < contents.length; i++) {
            boolean written = false;

            switch (contents[i]) {
                case "-t":
                    if(!trans) {
                        trans = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-d":
                case "-debug":
                    if(!debug) {
                        debug = true;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-m":
                case "-mode":
                    if(!mode && i < contents.length - 1) {
                        mode = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                case "-f":
                case "-fr":
                    if(!frame && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                        frame = true;
                        i++;
                    } else {
                        result.append(contents[i]);
                        written = true;
                    }
                    break;
                default:
                    result.append(contents[i]);
                    written = true;
            }

            if(written && i < contents.length - 1) {
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    private String getModeName(int mode, int max) {
        switch (mode) {
            case 1:
                return LangID.getStringByID("fimg_idle", lang);
            case 2:
                return LangID.getStringByID("fimg_atk", lang);
            case 3:
                return LangID.getStringByID("fimg_hitback", lang);
            case 4:
                if(max == 5)
                    return LangID.getStringByID("fimg_enter", lang);
                else
                    return LangID.getStringByID("fimg_burrowdown", lang);
            case 5:
                return LangID.getStringByID("fimg_burrowmove", lang);
            case 6:
                return LangID.getStringByID("fimg_burrowup", lang);
            default:
                return LangID.getStringByID("fimg_walk", lang);
        }
    }

    private List<String> accumulateData(List<Enemy> enemies) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= enemies.size())
                break;

            Enemy e = enemies.get(i);

            String ename;

            if(e.id != null) {
                ename = Data.trio(e.id.id)+" ";
            } else {
                ename = " ";
            }

            String name = StaticStore.safeMultiLangGet(e, lang);

            if(name != null)
                ename += name;

            data.add(ename);
        }

        return data;
    }
}
