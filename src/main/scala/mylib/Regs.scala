package mylib

import spinal.core._

import scala.collection.mutable
import scala.collection.mutable.Map

case class SimpleReg(name:String, size:Int=4,desc:String=""){

}

class RegMem(bitcount:BitCount,size:Int) extends Area{
  val data = Mem(Bits(bitcount),size).init(Array.fill(size)(B(0)))
  val outRange = Bool

  outRange := False

  val regsMap = mutable.LinkedHashMap[Int,SimpleReg]()

  def add(addr:Int,name:String,desc:String): Unit ={
    regsMap += ( addr-> SimpleReg(name,bitcount.value/8,desc))
  }

  val decodeMap = mutable.Map[(Component,UInt), UInt]()
  val accessMap = mutable.Map[(Component,UInt), Bits]()

  def decode(in:Int):Int={
    for( ((addr,_),index) <- regsMap.zipWithIndex){
      if(in==addr){
        return index
      }
    }
    throw new Exception("didn't find this addr in regs!")
  }

  def decode(in:UInt):UInt={
    val key = (Component.current,in)
    if(decodeMap.contains(key)){return decodeMap(key)}  // check whether we have decoded this addr variable before
    val out = UInt(log2Up(size) bits)
    switch(in){
      for( ((addr,_),i) <- regsMap.zipWithIndex){
        is(addr){
          out := i
        }
      }
      default{
        outRange := True
        out:=0
      }
    }
    decodeMap += key->out
    out
  }

  def apply(addr:Int)=data(decode(U(addr).resized))
  def apply(addr:UInt):Bits={
    val index = decode(addr)
    val key = (Component.current,index)
    if(accessMap.contains(key)){return accessMap(key)}

    // start to create access mux for this addr(decoded)
    // and we will save the mux in the accessMap, so that we can use it next time
    // refered the implement of Vec
    val origin = data(index)
    val ret = cloneOf(origin)
    ret := origin
    ret.compositeAssign =new Assignable {
      override  def assignFromImpl(that: AnyRef, target: AnyRef, kind: AnyRef) = {
        data(index) := that.asInstanceOf[Bits]
      }
      override def getRealSourceNoRec: Any = RegMem.this
    }
    accessMap += key->ret
    ret
  }

}
