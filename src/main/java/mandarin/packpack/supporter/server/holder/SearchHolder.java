package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchHolder extends InteractionHolder<GenericComponentInteractionCreateEvent> {
    public static final int PAGE_CHUNK = 20;

    protected final Message msg;
    protected final String channelID;
    protected final String memberID;
    protected final int lang;

    protected int page = 0;

    public SearchHolder(@Nonnull Message msg, @Nonnull String channelID, @Nonnull String memberID, int lang) {
        super(GenericComponentInteractionCreateEvent.class);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = memberID;
        this.lang = lang;
    }

    @Override
    public int handleEvent(GenericComponentInteractionCreateEvent event) {
        if(expired) {
            System.out.println("Expired!!");

            return RESULT_FAIL;
        }

        MessageChannel ch = event.getChannel();

        if(!ch.getId().equals(channelID))
            return RESULT_STILL;

        Member member = event.getInteraction().getMember();

        if(member == null || !member.getId().equals(memberID))
            return RESULT_STILL;

        Message m = event.getMessage();

        if(!msg.getId().equals(m.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        msg.editMessage(LangID.getStringByID("formst_expire", lang))
                .setActionRows()
                .queue();
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "prev10":
                page -= 10;

                break;
            case "prev":
                page--;

                break;
            case "next":
                page++;

                break;
            case "next10":
                page += 10;

                break;
            case "data":
                expired = true;

                StaticStore.removeHolder(event.getUser().getId(), this);

                onSelected(event);

                return;
            case "cancel":
                expired = true;

                StaticStore.removeHolder(event.getUser().getId(), this);

                event.deferEdit()
                        .setActionRows()
                        .complete();

                msg.editMessage(LangID.getStringByID("formst_cancel", lang)).complete();

                return;
        }

        apply(event);
    }

    @Override
    public void clean() {

    }

    public abstract List<String> accumulateListData(boolean onText);

    public abstract void onSelected(GenericComponentInteractionCreateEvent event);

    public abstract int getDataSize();

    private List<ActionRow> getComponents() {
        int totalPage = getDataSize() / PAGE_CHUNK + 1;

        List<ActionRow> rows = new ArrayList<>();

        if(getDataSize() > PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), Emoji.fromCustom(EmojiStore.TWO_PREVIOUS)));
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), Emoji.fromCustom(EmojiStore.PREVIOUS)));
            }

            if(page + 1 > getDataSize() / PAGE_CHUNK) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)).asDisabled());
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), Emoji.fromCustom(EmojiStore.NEXT)));
            }

            if(totalPage > 10) {
                if(page + 10 > getDataSize() / PAGE_CHUNK) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)).asDisabled());
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), Emoji.fromCustom(EmojiStore.TWO_NEXT)));
                }
            }

            rows.add(ActionRow.of(buttons));
        }

        List<SelectOption> options = new ArrayList<>();

        List<String> data = accumulateListData(false);

        for(int i = 0; i < data.size(); i++) {
            options.add(SelectOption.of(data.get(i), String.valueOf(page * PAGE_CHUNK + i)));
        }

        rows.add(ActionRow.of(SelectMenu.create("data").addOptions(options).setPlaceholder(LangID.getStringByID("search_list", lang)).build()));

        rows.add(ActionRow.of(Button.danger("cancel", LangID.getStringByID("button_cancel", lang))));

        return rows;
    }

    private String getPage() {
        StringBuilder sb = new StringBuilder("```md\n")
                .append(LangID.getStringByID("formst_pick", lang));

        List<String> data = accumulateListData(true);

        for(int i = 0; i < data.size(); i++) {
            sb.append(i + PAGE_CHUNK * page + 1)
                    .append(". ")
                    .append(data.get(i))
                    .append("\n");
        }

        if(getDataSize() > PAGE_CHUNK)
            sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(page + 1)).replace("-", String.valueOf(getDataSize() / PAGE_CHUNK + 1))).append("\n");

        sb.append("```");

        return sb.toString();
    }

    private void apply(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getPage())
                .setActionRows(getComponents())
                .complete();
    }
}