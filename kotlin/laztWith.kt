fun <C,M> lazyWith(build: (C) -> M) = LazyWith(build)

class LazyWith<C, M>(
    val build: (C) -> M
) : ReadOnlyProperty<C, M> {

    private val lock = Any()

    @Volatile
    private var INSTANCE: M? = null

    override fun getValue(thisRef: C, property: KProperty<*>): M {
        return INSTANCE ?: synchronized(lock) {
            if (INSTANCE == null) {
                INSTANCE = build(thisRef)
            }
            INSTANCE!!
        }
    }
}
