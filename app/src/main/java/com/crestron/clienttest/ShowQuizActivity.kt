package com.crestron.clienttest

class ShowQuizActivity : QuizActivity() {

    override val dialogHintText: String = "Choose a Source"
    override val dialogMessage: String = "Choose from Gogoanime, Putlocker, or Animetoon"

    override val postHighScoreLink: String? = null
    override val highScoreLink: String? = null

    override var dialogTitle: String = "Pick a Source"

    override fun onCreated() {
        titleText = "Show Quiz"
        type = QuizChoiceType.CHOICES
        setChoices("Gogoanime", "Putlocker", "Animetoon")
    }

    override fun getInfoLink(type: String): String = "/show/quiz/show_type=$type.json"

}