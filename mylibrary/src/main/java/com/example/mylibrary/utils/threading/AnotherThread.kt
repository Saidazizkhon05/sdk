package me.innerworks.iw_mobile_auth_android.utils.threading

import me.innerworks.iw_mobile_auth_android.utils.threading.safe.safe

/**
 * Returns immediately. The error relates to posting the job to another thread, not the error
 * that could have occurred on that thread.
 * If some Throwable is thrown from that thread, it will be ignored.
 */
internal fun runOnAnotherThread(
    block: () -> Unit,
): Result<Unit> = safe {
    sharedExecutor.submit { block() }
}
