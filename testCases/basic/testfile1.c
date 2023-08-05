float sb = 123;
int ab[1244] = {1,2,3};

int c(float a, float b, float c,float d, float e, float f,float g, float h, float i,float j, float k, float l,float m, float n,
float o,float p, float q, float r ){
    return a+b+c+d+r+f+g+h+i+j+k+l+m+n+o+p+q+r + sb;
}

int main() {
    int b = getint();
    int i = 3;
    while(i < 1000) {
        ab[i] = getint();
        i = i + 1;
    }
    int a = 1 + b;
    i = 0;
    while(i < 1000) {
        a  = a + ab[i];
        i = i + 1;
    }

    a = c(a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,b,b,b);
	return a + sb;
}
