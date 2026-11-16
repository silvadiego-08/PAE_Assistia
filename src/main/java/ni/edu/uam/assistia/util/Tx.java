package ni.edu.uam.assistia.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

/** Abre sesión, maneja transacción, hace rollback y traduce errores de BD a mensajes comprensibles. */
public final class Tx {

    private Tx() { }

    /** Operación de escritura dentro de una transacción. */
    public static <T> T ejecutar(Function<Session, T> trabajo) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                T resultado = trabajo.apply(s);
                tx.commit();
                return resultado;
            } catch (RuntimeException e) {
                try {
                    if (tx.isActive()) tx.rollback();
                } catch (RuntimeException ignorada) {
                    // se conserva el error original
                }
                throw e;
            }
        } catch (RuntimeException e) {
            throw ErroresBD.traducir(e);
        }
    }

    public static void ejecutarVoid(Consumer<Session> trabajo) {
        ejecutar(s -> {
            trabajo.accept(s);
            return null;
        });
    }

    /** Lectura sin transacción explícita. */
    public static <T> T leer(Function<Session, T> consulta) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return consulta.apply(s);
        } catch (RuntimeException e) {
            throw ErroresBD.traducir(e);
        }
    }
}
