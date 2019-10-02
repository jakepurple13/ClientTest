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

class MusicQuizActivity : QuizActivity() {

    override val dialogHintText: String = "Artist Name"
    override val dialogMessage: String = "Choose What Songs Can Be Shown"

    override val postHighScoreLink: String? = "/music"
    override val highScoreLink: String? = "/music/mobileHighScores.json"

    override var dialogTitle: String = "Choose an Artist/Band"

    override fun onCreated() {
        titleText = "Music Quiz"
    }

    override fun getInfoLink(type: String): String = "/music/music_get_quiz_from=$type.json"

}