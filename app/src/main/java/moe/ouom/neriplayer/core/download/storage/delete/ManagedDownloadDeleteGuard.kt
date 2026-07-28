package moe.ouom.neriplayer.core.download.storage.delete

import moe.ouom.neriplayer.core.download.storage.directory.ManagedDownloadDirectoryIdentity
import java.io.File

internal data class ManagedDownloadDeletePolicy(
    val managedFileRoots: List<String>,
    val managedTreeRoots: List<String>,
    val trustedReferences: Set<String>
)

internal object ManagedDownloadDeleteGuard {
    fun isReferenceAllowedForManagedDelete(
        reference: String,
        trustedReferences: Set<String>,
        managedFileRoots: Collection<String>,
        managedTreeRoots: Collection<String>,
        onTrustedReferenceOutsideManagedRoot: ((String) -> Unit)? = null
    ): Boolean {
        val normalizedReference = reference.takeIf(String::isNotBlank) ?: return false
        val isTrusted = normalizedReference in trustedReferences
        if (normalizedReference.startsWith("/")) {
            val underRoot = managedFileRoots.any { rootPath ->
                isFileReferenceUnderManagedRoot(normalizedReference, rootPath)
            }
            if (!underRoot && isTrusted) {
                onTrustedReferenceOutsideManagedRoot?.invoke(normalizedReference)
            }
            return underRoot
        }
        val underTree = managedTreeRoots.any { rootUri ->
            ManagedDownloadDirectoryIdentity.isDocumentReferenceUnderManagedTree(
                reference = normalizedReference,
                managedTreeUri = rootUri
            )
        }
        if (!underTree && isTrusted) {
            onTrustedReferenceOutsideManagedRoot?.invoke(normalizedReference)
        }
        return underTree
    }

    fun isFileReferenceUnderManagedRoot(reference: String, managedRootPath: String): Boolean {
        val root = runCatching { File(managedRootPath).canonicalFile }.getOrNull() ?: return false
        val target = runCatching { File(reference).canonicalFile }.getOrNull() ?: return false

        // ↓↓↓ 临时调试，跑完记得删 ↓↓↓
        println("DEBUG root.path   = [${root.path}] len=${root.path.length}")
        println("DEBUG target.path = [${target.path}] len=${target.path.length}")
        var p = target.parentFile
        var depth = 0
        while (p != null && depth < 10) {
            println("DEBUG parent[$depth].path = [${p.path}] equalsRoot=${p == root} equalsRootStr=${p.path.equals(root.path, ignoreCase = true)}")
            p = p.parentFile
            depth++
        }
        // ↑↑↑ 临时调试 ↑↑↑


        if (target == root) {
            return false
        }
        return generateSequence(target.parentFile) { file -> file.parentFile }
            .any { parent -> parent == root }
    }
}
