package io.paddle.plugin.python.extensions

import io.paddle.plugin.python.dependencies.authentication.AuthInfo
import io.paddle.plugin.python.dependencies.authentication.AuthType
import io.paddle.plugin.python.dependencies.repositories.PyPackageRepositories
import io.paddle.plugin.python.utils.PyPackagesRepositoryUrl
import io.paddle.project.Project
import io.paddle.utils.ext.Extendable
import io.paddle.utils.hash.Hashable
import io.paddle.utils.hash.hashable


val Project.repositories: Repositories
    get() = extensions.get(Repositories.Extension.key)!!

class Repositories(val project: Project, val descriptors: List<Descriptor>) : Hashable {

    val resolved: PyPackageRepositories by lazy { PyPackageRepositories.resolve(descriptors) }

    object Extension : Project.Extension<Repositories> {
        override val key: Extendable.Key<Repositories> = Extendable.Key()

        override fun create(project: Project): Repositories {
            val reposConfig = project.config.get<List<Map<String, Any>>>("repositories") ?: emptyList()

            val descriptors = reposConfig.map {
                val authType = AuthType.valueOf((it["auth"] as String? ?: "none").uppercase())
                val authInfo = if (authType == AuthType.PROFILE)
                    AuthInfo(authType, (it["auth"]!! as Map<*, *>)["profile"]!! as String)
                else
                    AuthInfo(authType)

                Descriptor(
                    it["name"]!! as String,
                    it["url"]!! as String,
                    (it["default"] as String?)?.toBoolean(),
                    (it["secondary"] as String?)?.toBoolean(),
                    authInfo,
                )
            }

            return Repositories(project, descriptors)
        }
    }

    data class Descriptor(
        val name: String,
        val url: PyPackagesRepositoryUrl,
        val default: Boolean?,
        val secondary: Boolean?,
        val authInfo: AuthInfo,
    ) : Hashable {
        override fun hash(): String {
            val hashables = mutableListOf(name.hashable(), url.hashable(), authInfo.toString().hashable())
            default?.let { hashables.add(it.hashable()) }
            secondary?.let { hashables.add(it.hashable()) }
            return hashables.hashable().hash()
        }

        companion object {
            val PYPI = Descriptor(
                name = "pypi",
                url = "https://pypi.org",
                default = true,
                secondary = false,
                authInfo = AuthInfo.NONE
            )
        }
    }

    override fun hash(): String {
        return descriptors.hashable().hash()
    }
}
