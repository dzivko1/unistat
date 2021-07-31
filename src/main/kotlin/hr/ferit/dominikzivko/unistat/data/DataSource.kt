package hr.ferit.dominikzivko.unistat.data

import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import java.util.*

/**
 * An [AppComponent] acting as a source of data required by the [Repository]. It provides general user data and bill
 * data while tracking progress with a [ProgressMonitor].
 *
 * Implementations can (but don't have to) mandate authentication in order to complete the fetching process. If such
 * requirements are imposed, the fetch methods are free to throw appropriate exceptions should the user fail to authenticate.
 */
interface DataSource : AppComponent {

    /** The current user's ID */
    val userID: UUID?

    /** Fetches the user's general data. Can track progress. */
    fun fetchGeneralData(progressMonitor: ProgressMonitor? = null): User

    /**
     * Fetches the user's bill data with awareness of the [existingBills] that are already present in the app.
     * Can track progress.
     */
    fun fetchBills(existingBills: List<Bill>, progressMonitor: ProgressMonitor? = null): List<Bill>

    /**
     * Revokes any and all rights to data access that a user might have through this data source, requiring fresh
     * authentication upon a next fetch if such authentication is mandated by the implementation.
     */
    fun revokeAuthorization()
}