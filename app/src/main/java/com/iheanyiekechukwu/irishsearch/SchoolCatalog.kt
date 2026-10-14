package com.iheanyiekechukwu.irishsearch

/** Campus configuration for this app. Other verified public PATH sites are documented separately. */
data class SchoolCatalog(
    val id: String,
    val name: String,
    val publicSite: String,
) {
    val api: String get() = "${publicSite}api/?page=fose&route=search"
    val detailsApi: String get() = "${publicSite}api/?page=fose&route=details"
}

object SchoolCatalogs {
    val notreDame = SchoolCatalog(
        id = "notre-dame",
        name = "University of Notre Dame",
        publicSite = "https://classsearch.nd.edu/",
    )

    val all = listOf(notreDame)

    fun byId(id: String): SchoolCatalog = all.firstOrNull { it.id == id } ?: notreDame
}
