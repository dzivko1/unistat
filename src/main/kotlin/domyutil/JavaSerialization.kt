package domyutil

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun serialize(obj: Any): ByteArray {
    return ByteArrayOutputStream().use { bos ->
        ObjectOutputStream(bos).use { oos ->
            oos.writeObject(obj)
            oos.flush()
            bos.toByteArray()
        }
    }
}

fun deserialize(bytes: ByteArray): Any {
    return ByteArrayInputStream(bytes).use { bis ->
        ObjectInputStream(bis).use { ois ->
            ois.readObject()
        }
    }
}
