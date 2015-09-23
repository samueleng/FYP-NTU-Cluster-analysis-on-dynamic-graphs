package MLRMCL;


public class IntContainer implements ContainerBuilder<IntContainer>{
    final int[] c;

    public IntContainer(int size) {
        this.c = new int[size];
    }
    @Override
   public IntContainer build(int dimension){
    	return new IntContainer(dimension);
    }
    
}

