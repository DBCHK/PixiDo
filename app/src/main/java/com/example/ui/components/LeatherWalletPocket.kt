package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.AccountEntity
import com.example.data.CardNetwork
import com.example.data.Currencies
import com.example.data.maskedPan
import com.example.data.resolvedExpiry
import com.example.data.resolvedHolder
import com.example.data.resolvedNetwork
import com.example.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private val PocketShape = RoundedCornerShape(32.dp)
private val PeekCardShape = RoundedCornerShape(20.dp)
private val PocketBodyHeight = 228.dp
private val PocketStampTop = 102.dp
private val LeatherWell = Color(0xCC140806)
private val FoilBrush = Brush.verticalGradient(
    0f to Color(0xFFF6E2A8),
    0.42f to Color(0xFFE1B34A),
    1f to Color(0xFF9A6A22)
)

@Composable
fun LeatherWalletPocket(
    cards: List<AccountEntity>,
    selectedIndex: Int,
    holderFallback: String,
    assetTotal: Double,
    netWorth: Double,
    creditUtilizedTotal: Double,
    currencyCode: String,
    spendingLabel: String,
    spendingVibe: String,
    spendingVibeColor: Color,
    hiddenAmounts: Boolean,
    showAfterCards: Boolean,
    onToggleHidden: () -> Unit,
    onToggleAfterCards: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    onCardClick: (AccountEntity) -> Unit,
    onAddCard: () -> Unit,
    onAddBalance: () -> Unit,
    onTransfer: () -> Unit,
    onAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = cards.getOrNull(selectedIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(318.dp)
            .testTag("wallet_accounts_entry")
    ) {
        PeekingCardStack(
            cards = cards,
            selectedIndex = selectedIndex,
            holderFallback = holderFallback,
            currencyCode = currencyCode,
            hiddenAmounts = hiddenAmounts,
            onSelectIndex = onSelectIndex,
            onCardClick = {
                if (selected != null) onCardClick(selected) else onAddCard()
            },
            onAddCard = onAddCard,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(0f)
                .padding(horizontal = 28.dp)
                .testTag("wallet_stacked_cards")
        )
        LeatherPocketBody(
            assetTotal = assetTotal,
            netWorth = netWorth,
            creditUtilizedTotal = creditUtilizedTotal,
            currencyCode = currencyCode,
            hasCards = cards.isNotEmpty(),
            spendingLabel = spendingLabel,
            spendingVibe = spendingVibe,
            spendingVibeColor = spendingVibeColor,
            hiddenAmounts = hiddenAmounts,
            showAfterCards = showAfterCards,
            onToggleHidden = onToggleHidden,
            onToggleAfterCards = onToggleAfterCards,
            onAddBalance = onAddBalance,
            onTransfer = onTransfer,
            onAccounts = onAccounts,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        )
    }
}

@Composable
private fun PeekingCardStack(
    cards: List<AccountEntity>,
    selectedIndex: Int,
    holderFallback: String,
    currencyCode: String,
    hiddenAmounts: Boolean,
    onSelectIndex: (Int) -> Unit,
    onCardClick: () -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var busy by remember { mutableStateOf(false) }
    val swipeAnim = remember {
        object {
            var job: Job? = null
            var gen: Int = 0
        }
    }
    val n = cards.size
    val front = cards.getOrNull(wrapWalletCardIndex(selectedIndex, n))
    val next = if (n >= 2) cards.getOrNull(wrapWalletCardIndex(selectedIndex + 1, n)) else null
    val prev = if (n >= 2) cards.getOrNull(wrapWalletCardIndex(selectedIndex - 1, n)) else null
    val tail = if (n >= 3) cards.getOrNull(wrapWalletCardIndex(selectedIndex + 2, n)) else null
    val maxDrag = (widthPx * 0.55f).coerceAtLeast(64f)
    val x = dragX
    val p = (abs(x) / maxDrag).coerceIn(0f, 1f)
    val riseNext = if (x < 0f) p else 0f
    val risePrev = if (x > 0f) p else 0f
    val settleSpec = if (reduceMotion) {
        tween<Float>(140, easing = FastOutSlowInEasing)
    } else {
        spring(dampingRatio = 0.92f, stiffness = 280f)
    }

    fun cancelSettle() {
        swipeAnim.gen++
        swipeAnim.job?.cancel()
        swipeAnim.job = null
        busy = false
    }

    fun animateDragTo(target: Float, onEnd: () -> Unit = {}) {
        swipeAnim.job?.cancel()
        val gen = ++swipeAnim.gen
        busy = true
        swipeAnim.job = scope.launch {
            try {
                val anim = Animatable(dragX)
                anim.animateTo(target, settleSpec) { dragX = value }
                if (gen == swipeAnim.gen) onEnd()
            } finally {
                if (gen == swipeAnim.gen) {
                    busy = false
                    swipeAnim.job = null
                }
            }
        }
    }

    fun springBack() = animateDragTo(0f) { dragX = 0f }

    fun commit(dir: WalletSwipeDir) {
        if (n < 2) {
            springBack()
            return
        }
        val toNext = dir == WalletSwipeDir.Next
        val newIndex = wrapWalletCardIndex(selectedIndex + if (toNext) 1 else -1, n)
        val target = (if (toNext) -1f else 1f) * maxDrag
        animateDragTo(target) {
            dragX = 0f
            onSelectIndex(newIndex)
        }
    }

    val dragState = rememberDraggableState { delta ->
        val rubber = if (n < 2) 0.22f else 1f
        dragX = walletRubberX(dragX + delta * rubber, maxDrag)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (abs(dragX) < 10f && !busy) {
                        if (front != null) onCardClick() else onAddCard()
                    }
                }
            )
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = n > 0,
                onDragStarted = { cancelSettle() },
                onDragStopped = { velocity ->
                    val dir = walletSwipeDecision(dragX, velocity, maxDrag, n)
                    if (dir != null) commit(dir) else springBack()
                }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        if (tail != null && tail.id != front?.id && tail.id != next?.id) {
            PeekingCardFace(
                account = tail,
                holderFallback = holderFallback,
                currencyCode = currencyCode,
                hiddenAmounts = hiddenAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = -16f
                        scaleX = 0.93f
                        scaleY = 0.93f
                        alpha = 0.72f
                    }
            )
        }
        if (next != null && n > 2) {
            PeekingCardFace(
                account = next,
                holderFallback = holderFallback,
                currencyCode = currencyCode,
                hiddenAmounts = hiddenAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(4f + riseNext * 7f)
                    .graphicsLayer {
                        translationY = -8f + riseNext * 8f
                        scaleX = 0.96f + riseNext * 0.04f
                        scaleY = 0.96f + riseNext * 0.04f
                        rotationZ = if (reduceMotion) 0f else (1f - riseNext) * -1.4f
                    }
            )
        }
        if (prev != null && n > 2) {
            PeekingCardFace(
                account = prev,
                holderFallback = holderFallback,
                currencyCode = currencyCode,
                hiddenAmounts = hiddenAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(3f + risePrev * 8f)
                    .graphicsLayer {
                        translationY = -10f + risePrev * 10f
                        scaleX = 0.94f + risePrev * 0.06f
                        scaleY = 0.94f + risePrev * 0.06f
                        alpha = risePrev
                        rotationZ = if (reduceMotion) 0f else (1f - risePrev) * 1.4f
                    }
            )
        }
        if (next != null && n == 2) {
            PeekingCardFace(
                account = next,
                holderFallback = holderFallback,
                currencyCode = currencyCode,
                hiddenAmounts = hiddenAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(4f + p * 7f)
                    .graphicsLayer {
                        translationY = -8f + p * 8f
                        scaleX = 0.96f + p * 0.04f
                        scaleY = 0.96f + p * 0.04f
                    }
            )
        }
        if (front != null) {
            PeekingCardFace(
                account = front,
                holderFallback = holderFallback,
                currencyCode = currencyCode,
                hiddenAmounts = hiddenAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(10f - p * 8f)
                    .graphicsLayer {
                        translationX = x
                        translationY = p * 10f
                        rotationZ = if (reduceMotion) 0f else (x / maxDrag) * 7f
                        scaleX = 1f - p * 0.05f
                        scaleY = 1f - p * 0.05f
                        alpha = 1f - p * 0.12f
                        cameraDistance = 18f * density
                    }
                    .testTag("wallet_peek_card")
            )
        } else {
            EmptyPeekCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .zIndex(8f)
            )
        }
    }
}

internal enum class WalletSwipeDir { Next, Previous }

internal fun wrapWalletCardIndex(index: Int, n: Int): Int {
    if (n <= 0) return 0
    return ((index % n) + n) % n
}

internal fun walletRubberX(raw: Float, limit: Float): Float {
    if (limit <= 0f) return 0f
    val a = abs(raw)
    if (a <= limit) return raw
    val extra = a - limit
    return sign(raw) * (limit + extra * 0.22f)
}

internal fun walletSwipeDecision(
    dragX: Float,
    velocityX: Float,
    maxDrag: Float,
    count: Int,
    flingVelocity: Float = 700f,
    distanceFraction: Float = 0.20f
): WalletSwipeDir? {
    if (count < 2) return null
    val fling = abs(velocityX) >= flingVelocity
    val far = abs(dragX) >= maxDrag * distanceFraction
    if (!fling && !far) return null
    val toNext = if (fling) velocityX < 0f else dragX < 0f
    return if (toNext) WalletSwipeDir.Next else WalletSwipeDir.Previous
}

@Composable
private fun PeekingCardFace(
    account: AccountEntity,
    holderFallback: String,
    currencyCode: String,
    hiddenAmounts: Boolean,
    modifier: Modifier = Modifier
) {
    val network = account.resolvedNetwork().let {
        if (it == CardNetwork.OTHER && account.isCreditCard) CardNetwork.VISA else it
    }
    val holder = account.resolvedHolder(holderFallback)
    val last4 = account.maskedPan()
    val expiry = account.resolvedExpiry()
    val nameColor = walletCardOnColor(network, account.id)
    val muted = nameColor.copy(alpha = 0.78f)
    val art = walletCardArtRes(network, account.id)

    Box(
        modifier = modifier
            .shadow(12.dp, PeekCardShape, ambientColor = Color(0x66000000), spotColor = Color(0x44000000))
            .clip(PeekCardShape)
    ) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, top = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = holder,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                )
                CardNetworkLogo(
                    network = network,
                    modifier = Modifier
                        .width(networkLogoWidth(network))
                        .height(networkLogoHeight(network))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = last4,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = muted,
                    letterSpacing = 1.8.sp
                )
                if (expiry.isNotBlank()) {
                    Text(
                        text = "Valid $expiry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = muted
                    )
                }
            }
        }
    }
}

private fun networkLogoWidth(network: CardNetwork) = when (network) {
    CardNetwork.MASTERCARD -> 48.dp
    CardNetwork.RUPAY -> 62.dp
    else -> 56.dp
}

private fun networkLogoHeight(network: CardNetwork) = when (network) {
    CardNetwork.MASTERCARD -> 28.dp
    else -> 18.dp
}

@Composable
private fun EmptyPeekCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(8.dp, PeekCardShape)
            .clip(PeekCardShape)
    ) {
        Image(
            painter = painterResource(R.drawable.card_black),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, top = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add a card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                CardNetworkLogo(
                    network = CardNetwork.VISA,
                    modifier = Modifier
                        .width(56.dp)
                        .height(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Visa  ·  Mastercard  ·  RuPay",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun LeatherPocketBody(
    assetTotal: Double,
    netWorth: Double,
    creditUtilizedTotal: Double,
    currencyCode: String,
    hasCards: Boolean,
    spendingLabel: String,
    spendingVibe: String,
    spendingVibeColor: Color,
    hiddenAmounts: Boolean,
    showAfterCards: Boolean,
    onToggleHidden: () -> Unit,
    onToggleAfterCards: () -> Unit,
    onAddBalance: () -> Unit,
    onTransfer: () -> Unit,
    onAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shown = if (showAfterCards) netWorth else assetTotal
    val split = Currencies.split(shown, currencyCode)
    val amountPrefix = if (split.negative) "−" else ""
    val amountText = if (hiddenAmounts) "••••••" else "$amountPrefix${split.whole}.${split.cents}"
    val label = when {
        showAfterCards -> "After card bills"
        else -> "Total Balance"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PocketBodyHeight)
            .shadow(
                elevation = 18.dp,
                shape = PocketShape,
                ambientColor = Color(0x664A2A18),
                spotColor = Color(0x444A2A18)
            )
            .clip(PocketShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAccounts
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF552415))
        )
        Image(
            painter = painterResource(R.drawable.wallet_pocket),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 18.dp, bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(PocketStampTop))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(36.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleAfterCards
                        )
                        .testTag("wallet_total_balance"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EngravedLeatherText(
                        text = label.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        EngravedLeatherText(
                            text = amountText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (!hiddenAmounts) {
                            Spacer(modifier = Modifier.width(6.dp))
                            EngravedLeatherText(
                                text = currencyCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .clickable(onClick = onToggleHidden)
                        .testTag("wallet_hide_balance"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hiddenAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (hiddenAmounts) "Show balance" else "Hide balance",
                        tint = Color(0xFFD9B56A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(if (showAfterCards) 1.dp else 0.dp)
                    .testTag("wallet_after_cards_balance")
            )
            Box(
                modifier = Modifier
                    .size(0.dp)
                    .testTag("wallet_spending_chip")
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable(onClick = onAddBalance)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                        .testTag("wallet_request_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Balance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                PocketIconButton(
                    icon = Icons.Filled.SwapHoriz,
                    contentDescription = "Transfer",
                    onClick = onTransfer,
                    modifier = Modifier.testTag("wallet_send_btn")
                )
                PocketIconButton(
                    icon = Icons.Filled.Link,
                    contentDescription = "Accounts",
                    onClick = onAccounts
                )
            }
        }
    }
}

@Composable
private fun EngravedLeatherText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = TextStyle(
            brush = FoilBrush,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textAlign = textAlign ?: TextAlign.Unspecified,
            shadow = Shadow(
                color = LeatherWell,
                offset = Offset(1.2f, 2f),
                blurRadius = 0.6f
            )
        )
    )
}

@Composable
private fun PocketIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
