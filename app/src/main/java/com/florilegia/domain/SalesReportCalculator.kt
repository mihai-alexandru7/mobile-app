package com.florilegia.domain

import com.florilegia.data.model.Book
import com.florilegia.data.model.Order

enum class SalesTimeRange(val label: String, val days: Long?) {
    ALL_TIME("All Time", null),
    LAST_7_DAYS("7 Days", 7L),
    LAST_30_DAYS("30 Days", 30L),
    LAST_90_DAYS("90 Days", 90L),
}

data class BookSalesRow(
    val bookId: String,
    val title: String,
    val coverUrl: String,
    val status: String,
    val unitPrice: Double,
    val copiesSold: Int,
    val revenue: Double,
)

data class SalesReport(
    val totalRevenue: Double = 0.0,
    val totalCopiesSold: Int = 0,
    val totalOrders: Int = 0,
    val bookRows: List<BookSalesRow> = emptyList(),
)

object SalesReportCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun calculate(
        publisherId: String,
        books: List<Book>,
        orders: List<Order>,
        timeRange: SalesTimeRange,
        nowMillis: Long = System.currentTimeMillis(),
    ): SalesReport {
        val cutoffMillis = timeRange.days?.let { nowMillis - it * DAY_MILLIS }

        val filteredOrders = orders.filter { order ->
            if (cutoffMillis == null) true
            else {
                val createdMs = order.createdAt?.toDate()?.time ?: nowMillis
                createdMs >= cutoffMillis
            }
        }

        val copiesByBook = mutableMapOf<String, Int>()
        val revenueByBook = mutableMapOf<String, Double>()
        val fallbackTitleByBook = mutableMapOf<String, String>()
        var totalRevenue = 0.0
        var totalCopies = 0
        var matchingOrders = 0

        for (order in filteredOrders) {
            val myItems = order.items.filter { it.publisherId == publisherId }
            if (myItems.isEmpty()) continue
            matchingOrders++
            for (item in myItems) {
                totalCopies++
                totalRevenue += item.priceAtPurchase
                copiesByBook[item.bookId] = (copiesByBook[item.bookId] ?: 0) + 1
                revenueByBook[item.bookId] = (revenueByBook[item.bookId] ?: 0.0) + item.priceAtPurchase
                if (item.title.isNotBlank()) {
                    fallbackTitleByBook[item.bookId] = item.title
                }
            }
        }

        val booksById = books.associateBy { it.id }
        val allBookIds = (books.map { it.id } + copiesByBook.keys).distinct()

        val rows = allBookIds.map { bookId ->
            val book = booksById[bookId]
            BookSalesRow(
                bookId = bookId,
                title = book?.title ?: fallbackTitleByBook[bookId] ?: "Book #${bookId.take(8)}",
                coverUrl = book?.coverUrl.orEmpty(),
                status = book?.status ?: "approved",
                unitPrice = book?.price ?: 0.0,
                copiesSold = copiesByBook[bookId] ?: 0,
                revenue = revenueByBook[bookId] ?: 0.0,
            )
        }.sortedWith(
            compareByDescending<BookSalesRow> { it.revenue }
                .thenByDescending { it.copiesSold }
                .thenBy { it.title.lowercase() }
        )

        return SalesReport(
            totalRevenue = totalRevenue,
            totalCopiesSold = totalCopies,
            totalOrders = matchingOrders,
            bookRows = rows,
        )
    }
}