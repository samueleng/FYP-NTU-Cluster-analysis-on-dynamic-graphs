package chinesewhispers;

import java.util.ArrayList;

public class Matrix<T> {

    final ArrayList<T> matrix;

    public Matrix(final int axis1, final int axis2, ContainerBuilder<T> builder) {
        matrix = new ArrayList<>(axis1);

        for (int i = 0; i < axis1; i++) {
            matrix.add(builder.build(axis2));
        }
    }

    public T get(int axis) {
        return matrix.get(axis);
    }
}
