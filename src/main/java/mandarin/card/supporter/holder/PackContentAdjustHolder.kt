package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardChancePairAmountHolder
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardGroupData
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class PackContentAdjustHolder(author: Message, channelID: String,
    private val message: Message,
    private val pack: CardPack,
    private val cardChancePairList: CardChancePairList,
    private val new: Boolean
) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of rolled cards")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("amount", "Rolled Cards Amount")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardChancePairAmountHolder(authorMessage, channelID, message.id, cardChancePairList))
            }
            "pair" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val pairList = cardChancePairList.pairs[index]

                connectTo(event, CardChancePairHolder(authorMessage, channelID, message, pack, cardChancePairList, pairList, false))
            }
            "add" -> {
                val pairList = CardChancePair(0.0, CardGroupData(ArrayList(), ArrayList()))

                connectTo(event, CardChancePairHolder(authorMessage, channelID, message, pack, cardChancePairList, pairList, true))
            }
            "create" -> {
                cardChancePairList.validateChance()

                pack.cardChancePairLists.add(cardChancePairList)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card/chance pair list to the pack! Check result above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card/chance pair list? This cannot be undone",
                        LangID.EN
                    )

                    StaticStore.removeHolder(authorMessage.author.id, this)

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                        e.deferEdit().queue()

                        goBack()

                        return@ConfirmPopUpHolder null
                    }, { e ->
                        StaticStore.putHolder(authorMessage.author.id, this)

                        applyResult(e)

                        return@ConfirmPopUpHolder null
                    }, LangID.EN))
                } else {
                    cardChancePairList.validateChance()

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card/chance pair list? This cannot be undone",
                    LangID.EN
                )

                StaticStore.removeHolder(authorMessage.author.id, this)

                StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                    pack.cardChancePairLists.remove(cardChancePairList)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully delete card/chance pair list! Check result above")
                        .setEphemeral(true)
                        .queue()

                    goBack()

                    return@ConfirmPopUpHolder null
                }, { e ->
                    StaticStore.putHolder(authorMessage.author.id, this)

                    applyResult(e)

                    return@ConfirmPopUpHolder null
                }, LangID.EN))
            }
        }
    }

    override fun onBack() {
        super.onBack()

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder("## Card/Chance Pair List Adjust Menu\nPack name : ")
            .append(pack.packName)
            .append("\n\nAmount : ")
            .append(cardChancePairList.amount)
            .append(" card")

        if (cardChancePairList.amount > 1)
            builder.append("s")

        builder.append("\n\n")

        if (cardChancePairList.pairs.isEmpty()) {
            builder.append("- No card/chance pairs")
        } else {
            for (i in cardChancePairList.pairs.indices) {
                builder.append(i + 1)
                    .append(". ")
                    .append(CardData.df.format(cardChancePairList.pairs[i].chance))
                    .append(" % : ")
                    .append(cardChancePairList.pairs[i].cardGroup.getName())

                if (i < cardChancePairList.pairs.size - 1)
                    builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("amount", "Adjust Card Amount")
        ))

        if (cardChancePairList.pairs.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            for (i in cardChancePairList.pairs.indices) {
                options.add(SelectOption.of((i + 1).toString(), i.toString()))
            }

            result.add(ActionRow.of(
                StringSelectMenu.create("pair")
                    .addOptions(options)
                    .setPlaceholder("Select card/chance pair to adjust it")
                    .build()
            ))
        }

        result.add(
            ActionRow.of(
                Button.secondary("add", "Add Card/Chance Pair")
                    .withEmoji(Emoji.fromUnicode("➕"))
                    .withDisabled(pack.cardChancePairLists.size >= StringSelectMenu.OPTIONS_MAX_AMOUNT)
            )
        )

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create"),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back"),
                    Button.danger("delete", "Delete")
                )
            )
        }

        return result
    }
}