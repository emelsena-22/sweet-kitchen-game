package com.student.recipes

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

// One dessert from assets/recipes.json
data class Recipe(
    val name: String,
    val category: String,
    val emoji: String,
    val ingredients: List<String>,
    val steps: List<String>
)

// One restaurant guest from assets/customers.json
data class Customer(val name: String, val face: String)

class MainActivity : Activity() {

    // ---------- Game data (loaded from assets) ----------
    private lateinit var recipes: List<Recipe>
    private lateinit var customers: List<Customer>
    private lateinit var ingredientEmoji: Map<String, String>
    private lateinit var allIngredients: List<String>

    // ---------- Game state ----------
    private var score = 0
    private var lives = 3
    private var level = 1
    private var served = 0
    private var streak = 0
    private var bestStreak = 0
    private var perfectDishes = 0
    private var mistakes = 0
    private var cleanRound = true
    private var recipe: Recipe? = null
    private var lastRecipe: Recipe? = null
    private var customer: Customer? = null
    private var isVip = false
    private val added = mutableListOf<String>()
    private val servedEmojis = mutableListOf<String>()
    private var timeTotalMs = 0L
    private var timeLeftMs = 0L
    private var ticking = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!ticking) return
            timeLeftMs -= TICK_MS
            updatePatience()
            if (timeLeftMs <= 0) customerLeaves() else handler.postDelayed(this, TICK_MS)
        }
    }

    // ---------- Views ----------
    private lateinit var startScreen: View
    private lateinit var gameScreen: View
    private lateinit var overScreen: View
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var livesText: TextView
    private lateinit var restaurantSign: TextView
    private lateinit var customerFace: TextView
    private lateinit var customerName: TextView
    private lateinit var bubble: TextView
    private lateinit var moodText: TextView
    private lateinit var patienceBar: ProgressBar
    private lateinit var timeText: TextView
    private lateinit var plateText: TextView
    private lateinit var chefText: TextView
    private lateinit var dishTitle: TextView
    private lateinit var needList: FlowLayout
    private lateinit var feedbackText: TextView
    private lateinit var fridge: FlowLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startScreen = findViewById(R.id.startScreen)
        gameScreen = findViewById(R.id.gameScreen)
        overScreen = findViewById(R.id.overScreen)
        scoreText = findViewById(R.id.scoreText)
        levelText = findViewById(R.id.levelText)
        livesText = findViewById(R.id.livesText)
        restaurantSign = findViewById(R.id.restaurantSign)
        customerFace = findViewById(R.id.customerFace)
        customerName = findViewById(R.id.customerName)
        bubble = findViewById(R.id.bubble)
        moodText = findViewById(R.id.moodText)
        patienceBar = findViewById(R.id.patienceBar)
        timeText = findViewById(R.id.timeText)
        plateText = findViewById(R.id.plateText)
        chefText = findViewById(R.id.chefText)
        dishTitle = findViewById(R.id.dishTitle)
        needList = findViewById(R.id.needList)
        feedbackText = findViewById(R.id.feedbackText)
        fridge = findViewById(R.id.fridge)

        loadData()
        styleViews()

        findViewById<Button>(R.id.startButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.againButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.menuButton).setOnClickListener { showStart() }

        showStart()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onResume() {
        super.onResume()
        if (ticking) handler.postDelayed(tick, TICK_MS)
    }

    // ---------- Loading ----------

    private fun loadData() {
        val recipeArray = JSONArray(readAsset("recipes.json"))
        recipes = (0 until recipeArray.length()).map { i ->
            val o = recipeArray.getJSONObject(i)
            val ing = o.getJSONArray("ingredients")
            val steps = o.getJSONArray("steps")
            Recipe(
                o.getString("name"),
                o.getString("category"),
                o.getString("emoji"),
                (0 until ing.length()).map { ing.getString(it) }.distinct(),
                (0 until steps.length()).map { steps.getString(it) }
            )
        }

        val customerArray = JSONArray(readAsset("customers.json"))
        customers = (0 until customerArray.length()).map { i ->
            val o = customerArray.getJSONObject(i)
            Customer(o.getString("name"), o.getString("face"))
        }

        val emojiObject = JSONObject(readAsset("ingredients.json"))
        ingredientEmoji = emojiObject.keys().asSequence().associateWith { emojiObject.getString(it) }

        allIngredients = recipes.flatMap { it.ingredients }.distinct()
    }

    private fun readAsset(name: String) =
        assets.open(name).bufferedReader().use { it.readText() }

    private fun emojiFor(ingredient: String) = ingredientEmoji[ingredient] ?: "🥄"

    // ---------- Restaurant look ----------

    private fun styleViews() {
        val logo = "Sweet <font color='#672A39'>Kitchen</font>"
        findViewById<TextView>(R.id.startLogo).apply {
            text = Html.fromHtml(logo, Html.FROM_HTML_MODE_LEGACY)
            setTextColor(getColor(R.color.green))
        }
        findViewById<View>(R.id.howCard).background = rounded(R.color.white, 18f)

        // Warm restaurant wall
        findViewById<View>(R.id.scene).background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(getColor(R.color.wall_top), getColor(R.color.wall_bottom))
        )
        findViewById<View>(R.id.counterTop).setBackgroundColor(getColor(R.color.counter_top))
        findViewById<View>(R.id.counter).setBackgroundColor(getColor(R.color.counter))
        plateText.setTextColor(getColor(R.color.white))

        bubble.background = rounded(R.color.bubble, 16f).apply {
            setStroke(dp(2), getColor(R.color.counter_top))
        }
        findViewById<View>(R.id.ticket).background = rounded(R.color.white, 14f).apply {
            setStroke(dp(2), getColor(R.color.green_light))
        }
        findViewById<View>(R.id.fridgeBox).background = rounded(R.color.fridge, 18f).apply {
            setStroke(dp(3), getColor(R.color.white))
        }
        findViewById<TextView>(R.id.statsText).background = rounded(R.color.white, 16f)
        restaurantSign.text = "♥ SWEET KITCHEN BAKERY ♥"
    }

    // ---------- Screens ----------

    private fun showStart() {
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        startScreen.visibility = View.VISIBLE
        gameScreen.visibility = View.GONE
        overScreen.visibility = View.GONE
        val best = bestScore()
        findViewById<TextView>(R.id.bestText).text =
            if (best > 0) "🏆 Best score: $best" else "🏆 No high score yet — be the first!"
    }

    private fun startGame() {
        handler.removeCallbacksAndMessages(null)
        score = 0
        lives = 3
        level = 1
        served = 0
        streak = 0
        bestStreak = 0
        perfectDishes = 0
        mistakes = 0
        servedEmojis.clear()
        lastRecipe = null

        startScreen.visibility = View.GONE
        overScreen.visibility = View.GONE
        gameScreen.visibility = View.VISIBLE
        updateTopBar()
        nextCustomer()
    }

    // ---------- Game flow ----------

    private fun nextCustomer() {
        var r = recipes.random()
        while (r == lastRecipe) r = recipes.random()
        recipe = r
        lastRecipe = r
        val c = customers.random()
        customer = c
        added.clear()
        cleanRound = true

        // Bigger dishes get more time, higher levels get less
        val seconds = maxOf(6, 6 + r.ingredients.size * 3 - (level - 1))
        timeTotalMs = seconds * 1000L
        timeLeftMs = timeTotalMs

        customerFace.text = c.face
        // About 1 in 5 guests is a VIP who pays double
        isVip = Math.random() < VIP_CHANCE
        customerName.text = if (isVip) "👑 VIP ${c.name}" else c.name
        val greeting = if (isVip) "I'm a VIP guest! 👑 I'd like" else "Hi chef! One"
        bubble.text = Html.fromHtml(
            "$greeting <b>${r.name}</b> ${r.emoji}, please!",
            Html.FROM_HTML_MODE_LEGACY
        )
        chefText.text = "🧑‍🍳"
        feedbackText.text = ""
        dishTitle.text = "🧾 Order: ${r.emoji} ${r.name}  ·  ${r.category}"

        // Customer walks in from the left
        customerFace.translationX = -dp(220).toFloat()
        customerFace.animate().translationX(0f).setDuration(450).start()

        buildFridge(r)
        updatePlate()
        updateTopBar()
        startTimer()
    }

    private fun buildFridge(r: Recipe) {
        val extra = minOf(3 + level, 7)
        val distractors = allIngredients.filter { it !in r.ingredients }.shuffled().take(extra)
        val items = (r.ingredients + distractors).shuffled()

        fridge.removeAllViews()
        for (item in items) {
            fridge.addView(TextView(this).apply {
                text = "${emojiFor(item)}  $item"
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.text))
                background = rounded(R.color.white, 14f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                elevation = dp(2).toFloat()
                setOnClickListener { tapIngredient(item, this) }
            })
        }
    }

    private fun tapIngredient(item: String, button: TextView) {
        val r = recipe ?: return
        if (!ticking || item in added) return

        if (item in r.ingredients) {
            added.add(item)
            score += POINTS_CORRECT
            button.background = rounded(R.color.green, 14f)
            button.setTextColor(getColor(R.color.white))
            button.text = "✓ ${emojiFor(item)}  $item"
            button.isEnabled = false
            feedback("+$POINTS_CORRECT  ${emojiFor(item)} Good pick!", R.color.green)
            pop(button)
            updatePlate()
            updateTopBar()
            if (added.size == r.ingredients.size) serveDish()
        } else {
            score = maxOf(0, score - POINTS_WRONG)
            mistakes++
            cleanRound = false
            timeLeftMs -= 1000
            button.background = rounded(R.color.red_light, 14f)
            button.setTextColor(getColor(R.color.red))
            feedback("−$POINTS_WRONG  ${emojiFor(item)} $item is not in ${r.name}!", R.color.red)
            shake(button)
            chefText.text = "😵"
            handler.postDelayed({ if (ticking) chefText.text = "🧑‍🍳" }, 600)
            updateTopBar()
        }
    }

    // All ingredients are in: bake it first, then serve
    private fun serveDish() {
        val r = recipe ?: return
        val secondsLeft = (timeLeftMs / 1000).toInt()
        stopTimer()
        disableFridge()
        plateText.text = "🔥 Baking ${r.emoji} in the oven…"
        chefText.text = "🧑‍🍳🔥"
        feedback("🔥 Into the oven!", R.color.orange)
        handler.postDelayed({ finishServe(r, secondsLeft) }, BAKE_MS)
    }

    private fun finishServe(r: Recipe, secondsLeft: Int) {
        if (cleanRound) {
            streak++
            perfectDishes++
        } else {
            streak = 0
        }
        bestStreak = maxOf(bestStreak, streak)
        val multiplier = streak.coerceIn(1, 3)
        val timeBonus = secondsLeft * 2
        val vipFactor = if (isVip) 2 else 1
        val points = (POINTS_DISH + timeBonus) * multiplier * vipFactor
        score += points
        served++
        servedEmojis.add(r.emoji)

        val oldLevel = level
        level = served / DISHES_PER_LEVEL + 1

        customerFace.text = "😋"
        bubble.text = "Delicious! Thank you, chef! ⭐⭐⭐\n💡 Chef's tip: ${r.steps.randomOrNull() ?: "Bake with love!"}"
        chefText.text = "🥳"
        plateText.text = "🍽️ ${r.emoji} Served!"
        val combo = if (multiplier > 1) "  🎀 x$multiplier combo!" else ""
        val vip = if (isVip) "  👑 VIP x2!" else ""
        feedback("🍰 ${r.name} served! +$points (time bonus +$timeBonus)$combo$vip", R.color.green)
        if (level > oldLevel) {
            handler.postDelayed({ feedback("⭐ LEVEL $level! Guests are getting hungrier!", R.color.orange) }, 900)
        }
        updateTopBar()
        pop(scoreText)
        handler.postDelayed({ nextCustomer() }, NEXT_DELAY_MS)
    }

    private fun customerLeaves() {
        stopTimer()
        lives--
        streak = 0
        customerFace.text = "😠"
        bubble.text = "Too slow! I'm leaving… 😤"
        chefText.text = "😰"
        feedback("💔 ${customer?.name ?: "The customer"} left hungry! You lost a heart.", R.color.red)
        shake(customerFace)
        disableFridge()
        updateTopBar()
        handler.postDelayed({ if (lives <= 0) gameOver() else nextCustomer() }, NEXT_DELAY_MS)
    }

    private fun gameOver() {
        val oldBest = bestScore()
        val newRecord = score > oldBest
        if (newRecord) {
            getSharedPreferences("game", MODE_PRIVATE).edit().putInt("best", score).apply()
        }

        gameScreen.visibility = View.GONE
        overScreen.visibility = View.VISIBLE

        findViewById<TextView>(R.id.finalScore).text = "$score points"
        findViewById<TextView>(R.id.rankText).text = rankFor(score)
        findViewById<TextView>(R.id.statsText).text = buildString {
            append("🍽️  Dishes served: $served\n")
            append("✨  Perfect dishes (no mistakes): $perfectDishes\n")
            append("🔥  Best combo streak: $bestStreak\n")
            append("❌  Wrong ingredients: $mistakes\n")
            append("⭐  Level reached: $level\n")
            append(if (newRecord) "🎉  NEW HIGH SCORE!" else "🏆  Best score: $oldBest")
        }
        findViewById<TextView>(R.id.servedList).text =
            if (servedEmojis.isEmpty()) "🍽️" else servedEmojis.joinToString(" ")
    }

    private fun rankFor(points: Int) = when {
        points < 100 -> "🧽 Dishwasher"
        points < 300 -> "🍪 Cookie Cutter"
        points < 600 -> "🧁 Cupcake Baker"
        points < 1000 -> "🎂 Pastry Chef"
        else -> "👑 Sweet Kitchen Master"
    }

    private fun bestScore() = getSharedPreferences("game", MODE_PRIVATE).getInt("best", 0)

    // ---------- Timer ----------

    private fun startTimer() {
        ticking = true
        updatePatience()
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
    }

    private fun stopTimer() {
        ticking = false
        handler.removeCallbacks(tick)
    }

    private fun updatePatience() {
        val left = timeLeftMs.coerceAtLeast(0)
        val ratio = left.toFloat() / timeTotalMs
        patienceBar.progress = (ratio * 1000).toInt()
        timeText.text = "⏱ ${(left + 999) / 1000}s"

        val (mood, color) = when {
            ratio > 0.6f -> "😊" to R.color.ok
            ratio > 0.3f -> "😐" to R.color.orange
            else -> "😠" to R.color.red
        }
        moodText.text = mood
        patienceBar.progressTintList = ColorStateList.valueOf(getColor(color))
    }

    // ---------- UI helpers ----------

    private fun updateTopBar() {
        scoreText.text = "⭐ $score"
        levelText.text = "Level $level" + if (streak > 1) "  🎀x${streak.coerceAtMost(3)}" else ""
        livesText.text = "❤️".repeat(lives.coerceAtLeast(0)) + "🤍".repeat((3 - lives).coerceAtLeast(0))
    }

    private fun updatePlate() {
        val r = recipe ?: return
        plateText.text = "🍽️ " + added.joinToString(" ") { emojiFor(it) }

        needList.removeAllViews()
        for (item in r.ingredients) {
            val done = item in added
            needList.addView(TextView(this).apply {
                text = if (done) "✓ ${emojiFor(item)} $item" else "◻ $item"
                textSize = 13f
                setTextColor(getColor(if (done) R.color.green else R.color.muted))
                background = rounded(if (done) R.color.green_light else R.color.orange_light, 999f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
            })
        }
    }

    private fun disableFridge() {
        for (i in 0 until fridge.childCount) fridge.getChildAt(i).isEnabled = false
    }

    private fun feedback(message: String, color: Int) {
        feedbackText.text = message
        feedbackText.setTextColor(getColor(color))
        feedbackText.alpha = 0f
        feedbackText.animate().alpha(1f).setDuration(200).start()
    }

    private fun pop(view: View) {
        view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(120).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun shake(view: View) {
        view.animate().translationX(dp(12).toFloat()).setDuration(60).withEndAction {
            view.animate().translationX(-dp(12).toFloat()).setDuration(60).withEndAction {
                view.animate().translationX(0f).setDuration(60).start()
            }.start()
        }.start()
    }

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(getColor(color))
        cornerRadius = radiusDp * resources.displayMetrics.density
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TICK_MS = 100L
        private const val POINTS_CORRECT = 10
        private const val POINTS_WRONG = 5
        private const val POINTS_DISH = 50
        private const val DISHES_PER_LEVEL = 3
        private const val NEXT_DELAY_MS = 2600L
        private const val BAKE_MS = 900L
        private const val VIP_CHANCE = 0.2
    }
}
