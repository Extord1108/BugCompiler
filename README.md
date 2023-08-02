# BugCompiler

## Todo

### zmm

- [x] visitLVal完善
- [x] visitVarDef完善，补0，补未定义
### gyf
- [x] 关于evalExp类型传递的修正
- [x] VarArray的changeType对于Exp返回的Instr的兼容
- [x] Binary中的类型检查
- [x] Branch往后的tostring的实现

### bug
28 e-1词法错误

生成失败:/root/compiler/testcases/hidden_functional/23_json  需要添加zero区域赋值为0的操作

## 实现细节
关于未显式初始化的局部变量，对于数组元素不足的情况，任采用补0的方式进行，并不是不确定的

## 参考资料
MIR和LIR的关系：http://www.manongjc.com/detail/40-pdrghkceirlxalk.html

LLVM官方文档：https://llvm.org/docs/index.html

ARM条件执行：https://www.cnblogs.com/frankfankk/p/15738463.html