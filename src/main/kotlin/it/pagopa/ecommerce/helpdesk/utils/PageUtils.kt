package it.pagopa.ecommerce.helpdesk.utils

object PageUtils {
    /**
     * Calculate pages for display all records given a page size. Return a Pair<Int,Int> where first
     * argument is page size, second one is total count /page size remainder
     */
    fun calculatePages(pageSize: Int, totalCount: Int): Pair<Int, Int> {
        val remainder = totalCount % pageSize
        val pages = totalCount / pageSize
        return if (remainder == 0) {
            Pair(pages, remainder)
        } else {
            Pair(pages + 1, remainder)
        }
    }
}
