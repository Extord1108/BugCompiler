const float bb = 20;
int main() {
  int a = 2;
  const int b = 20;
  const float cc = 1.0;
  int c[b] = {1, 2, 0};
  int d = 0;
  while (a < b) {
    c[a] = c[a] + c[a - 1] + c[a - 2];
    d = d + c[a];
    putint(c[a]);
    putch(10);
    a = a + 1;
  }
  return d;
}