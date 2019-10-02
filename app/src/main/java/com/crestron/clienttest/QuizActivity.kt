package com.crestron.clienttest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import kotlinx.android.synthetic.main.activity_quiz.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class QuizChoiceType {
    TEXT, CHOICES
}

abstract class QuizActivity : AppCompatActivity() {

    abstract fun getInfoLink(type: String): String
    abstract val dialogTitle: String
    abstract val postHighScoreLink: String?
    abstract val highScoreLink: String?
    abstract val dialogHintText: String
    abstract val dialogMessage: String
    var titleText: String = "Quiz"
        set(value) {
            field = value
            runOnUiThread {
                title_text.text = titleText
            }
        }
    var type = QuizChoiceType.TEXT
    private var choices = mutableListOf<String>()

    abstract fun onCreated()

    open fun nextQuestionAction() {}
    open fun previousQuestionAction() {}
    open fun answerChecking() {}

    fun setChoices(vararg s: String) {
        choices.addAll(s)
    }

    private val client = HttpClient()
    private lateinit var quizQuestions: Array<QuizQuestions>
    private var counter = 0
        @SuppressLint("SetTextI18n")
        set(value) {
            field = value
            runOnUiThread {
                counterText.text = "${counter + 1}/${quizQuestions.size}"
            }
            if (counter + 1 == quizQuestions.size) {
                finished = true
            }
        }
    private val answerList = mutableMapOf<Int, Pair<Int, String>>()
    private var finished = false
        set(value) {
            field = value
            runOnUiThread {
                doneButton.visibility = if (field) View.VISIBLE else View.GONE
            }
        }
    private var artist = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        onCreated()

        getInfo()

        getHighScores()

        nextButton.setOnClickListener { nextQuestion() }
        prevButton.setOnClickListener { prevQuestion() }
        doneButton.setOnClickListener { answerCheck() }
    }

    private fun getHighScores() {
        if (!highScoreLink.isNullOrBlank()) {
            GlobalScope.launch {
                val s = client.get<String>(highScoreLink!!) {
                    method = HttpMethod.Get
                    host = ClientHandler.host
                    port = 8080
                }
                runOnUiThread {
                    highScoreTable.text = s
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getInfo() {
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val artistInput: View

        when (type) {
            QuizChoiceType.TEXT -> {
                artistInput = EditText(this)
                artistInput.hint = dialogHintText
                artistInput.imeOptions = EditorInfo.IME_ACTION_NEXT

            }
            QuizChoiceType.CHOICES -> {
                if (choices.isEmpty())
                    throw Exception("You don't have any choices!")
                artistInput = Spinner(this)
                artistInput.adapter =
                    ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, choices)
            }
        }

        artistInput.layoutParams = lp

        linearLayout.addView(artistInput)

        val builder = AlertDialog.Builder(this)
        builder.setView(linearLayout)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)
        builder.setCancelable(false)
        // Add the buttons
        builder.setPositiveButton("Okay!") { _, _ ->
            GlobalScope.launch {
                val chosen = when (type) {
                    QuizChoiceType.TEXT -> (artistInput as EditText).text.toString()
                    QuizChoiceType.CHOICES -> (artistInput as Spinner).adapter.getItem(artistInput.selectedItemPosition)!!.toString()
                }
                artist = chosen
                val choice = getInfoLink(chosen)
                val s = client.get<String>(choice) {
                    method = HttpMethod.Get
                    host = ClientHandler.host
                    port = 8080
                }
                quizQuestions = Gson().fromJson(s, Array<QuizQuestions>::class.java)
                answerList.clear()
                runOnUiThread {
                    counter = 0
                    finished = false
                    counterText.text = "$counter/${quizQuestions.size}"
                    setQuestionUp()
                }
            }
        }
        builder.setNegativeButton("Never Mind") { _, _ ->
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }

    data class QuizQuestions(
        val question: String,
        val choices: List<String>,
        val correctAnswer: String
    )

    data class UserInfo(val name: String, val artist: String, val score: String)

    private fun answerCheck() {
        answerChecking()
        val infoList = arrayListOf<String>()
        var count = 0
        for (q in quizQuestions.withIndex()) {
            Loged.r("${q.value}\n${answerList[q.index]}")
            if (q.value.correctAnswer == answerList[q.index]?.second) {
                count++
            }
            infoList += "${q.index}) Your Pick: ${answerList[q.index]?.second} | Correct Answer: ${q.value.correctAnswer}"
        }
        Loged.d("Your score is $count")

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val scoreView = ListView(this)
        scoreView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            300
        )
        scoreView.adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            infoList
        )

        linearLayout.addView(scoreView)

        val userInput = EditText(this)

        if (!postHighScoreLink.isNullOrBlank()) {
            userInput.layoutParams = lp
            userInput.hint = "Your Name (for the high score list)"
            userInput.imeOptions = EditorInfo.IME_ACTION_NEXT
            linearLayout.addView(userInput)
        }

        val builder = AlertDialog.Builder(this)
        builder.setView(linearLayout)
        builder.setTitle("Score")
        builder.setMessage("You got $count/${quizQuestions.size}")
        builder.setCancelable(false)
        // Add the buttons
        if (!postHighScoreLink.isNullOrBlank()) {
            builder.setPositiveButton("Submit Score") { _, _ ->
                GlobalScope.launch {
                    val s = client.post<String>(postHighScoreLink!!) {
                        method = HttpMethod.Post
                        host = ClientHandler.host
                        port = 8080
                        header("Content-type", "application/json")
                        body = UserInfo(
                            userInput.text.toString(),
                            artist,
                            "$count/${quizQuestions.size}"
                        ).toJson()
                    }
                    Loged.i(s)
                    getHighScores()
                }
                getInfo()
            }
        }
        builder.setNeutralButton("Stop Playing") { _, _ ->
            finish()
        }
        builder.setNegativeButton("Play Again!") { _, _ ->
            getInfo()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun answerSet() {
        try {
            answerList[counter] = Pair(
                answerSection.checkedRadioButtonId,
                findViewById<RadioButton>(answerSection.checkedRadioButtonId).text.toString()
                    .removePrefix("A) ")
                    .removePrefix("B) ")
                    .removePrefix("C) ")
                    .removePrefix("D) ")
            )
            answerSection.clearCheck()
        } catch (e: IllegalStateException) {

        }
    }

    private fun nextQuestion() {
        answerSet()
        if (counter + 1 <= quizQuestions.size - 1)
            counter += 1
        setQuestionUp()
        nextQuestionAction()
    }

    private fun prevQuestion() {
        answerSet()
        if (counter - 1 >= 0)
            counter -= 1
        setQuestionUp()
        previousQuestionAction()
    }

    @SuppressLint("SetTextI18n")
    private fun setQuestionUp() {
        answerList[counter]?.let {
            answerSection.check(it.first)
        }
        val question = quizQuestions[counter]
        snippetText.text = question.question
        answerA.text = "A) ${question.choices[0]}"
        answerB.text = "B) ${question.choices[1]}"
        answerC.text = "C) ${question.choices[2]}"
        answerD.text = "D) ${question.choices[3]}"
    }

}