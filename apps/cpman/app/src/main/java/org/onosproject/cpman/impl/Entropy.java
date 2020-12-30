package org.onosproject.cpman.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Entropy {
    public static List<Double> getWeight(List<List<Double>> list){
        List<Double> listSum = new ArrayList<Double>();	//用于存放每种指标下所有记录归一化后的和
        List<Double> gList = new ArrayList<Double>();	//用于存放每种指标的差异系数
        List<Double> wList = new ArrayList<Double>();	//用于存放每种指标的最终权重系数
        double sumLast = 0;
        double k = 1 / Math.log(list.get(0).size()); //计算k值 k= 1/ln(n)
        //数据归一化处理	(i-min)/(max-min)
        for (int i = 0; i < list.size(); i++) {
            double sum = 0;
            double max = Collections.max(list.get(i));
            double min = Collections.min(list.get(i));
            for (int j = 0; j <list.get(i).size(); j++) {
                double temp = (list.get(i).get(j) - min) / (max - min);
                sum += temp;
                list.get(i).set(j, temp);
            }
            listSum.add(sum);
        }


        //计算每项指标下每个记录所占比重
        for (int i = 0; i < list.size(); i++) {
            double sum = 0;	//每种指标下所有记录权重和
            for (int j = 0; j <list.get(i).size(); j++) {
                if(list.get(i).get(j) / listSum.get(i) == 0){
                    sum +=0;
                }else{
                    sum += (list.get(i).get(j) / listSum.get(i)) * Math.log(list.get(i).get(j) / listSum.get(i));
                }

            }
            //计算第i项指标的熵值
            double e = -k * sum;
            //计算第j项指标的差异系数
            double g = 1-e;
            sumLast += g;
            gList.add(g);
        }


        //计算每项指标的权重
        for (int i = 0; i < gList.size(); i++) {
            wList.add(gList.get(i) / sumLast);
        }

        return wList;
    }

}
