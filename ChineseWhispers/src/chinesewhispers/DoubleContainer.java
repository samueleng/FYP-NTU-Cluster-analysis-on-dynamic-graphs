package chinesewhispers;

public class DoubleContainer implements ContainerBuilder<DoubleContainer>{
    final double[] c;
    public DoubleContainer(int size) {
        this.c = new double[size];
    }  
    @Override
    public DoubleContainer build(int dimension){
    	return new DoubleContainer(dimension);
    }
}
