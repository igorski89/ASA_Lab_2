import Jama.Matrix;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashSet;

import static java.lang.Math.*;
import static java.lang.Math.*;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by IntelliJ IDEA.
 * User: igorevsukov
 * Date: Dec 3, 2009
 * Time: 7:03:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ASASample extends MDSample {
    protected double alpha = 0.05;
    /**
     * @return помилка першого роду
     */
    public double getAlpha() { return alpha; }
    /**
     * встановлює нове значення помилки першого роду для вибірки
     * @param newAlpha нове значення помилки першого роду
     */
    public void setAlpha(double newAlpha){ alpha = newAlpha; }

    public ASASample(int d) {
        super(d);
        pairedCorelations = new Criterion[d][d];
        partialCorelations = new Criterion[d][d];
        multipleCorelations = new Criterion[d];
        regressionCoeficients = new Criterion[d-1];
    }

    public void calculateParams() {
        super.calculateParams();

        calculatePairedCorelations();

        calculatePartialCorelation();

        calculateMultipleCorelations();

        calculateRegressionCoeficients();
    }

    protected Criterion pairedCorelations[][];
    public Criterion[][] getPairedCorelations() { return pairedCorelations; }
    public void calculatePairedCorelations() {
        final int n = this.getDimension();
        final int N = this.size();
        final double quantT = Quantiles.t(alpha/2.0,N-2);
        for (int i=0; i<n; i++) {
            for(int j=0; j<n; j++) {
                if (pairedCorelations[i][j] == null) pairedCorelations[i][j] = new Criterion(String.format("r[%d,%d]",i,j));
                Criterion r = pairedCorelations[i][j];

                if (i==j) {
                    r.setValue(1.0);
                }
                else {
                    double meanij = 0;
                    for (MDObject x:data) meanij += x.getParam(i)*x.getParam(j);
                    meanij /= N;

                    r.setValue((meanij-mean[i]*mean[j])/(sigma[i]*sigma[j]));
                }
                r.setStatistic(sqrt(N - 2.0) * r.getValue()/ sqrt(1 - pow(r.getValue(), 2)));
                r.setQuantile(quantT);
                r.setSignificant(!(abs(r.getStatistic())<=quantT));
            }
        }
    }


    /**
     * @param i
     * @param j
     * @param d
     * @return значення часткового коефіцієнту кореляції для i,j,d - змінні з множини X
     */
    protected double getPartialCorelationValue(int i, int j, int d) {
        Criterion rij = pairedCorelations[i][j];
        if (rij == null) calculatePairedCorelations();

        Criterion rid = pairedCorelations[i][d];
        Criterion rjd = pairedCorelations[j][d];

        double rijd = ( rij.getValue() - rid.getValue()*rjd.getValue() )/sqrt( (1-pow(rid.getValue(),2))*(1-pow(rjd.getValue(),2)) );
        return rijd;
    }
    /**
     * @param i
     * @param j
     * @param c
     * @return значення часткового коефіцієнту кореляції для i,j - змінні з множини X
     * c - множина змінних з X, що не містить i та j
     */
    protected double getPartialCorelationValue(int i, int j, HashSet<Integer> c) {
        if (c.size() == 1) {
            int d = (Integer)c.toArray()[0];
            return getPartialCorelationValue(i, j, d);
        }
        else {
            int d = (Integer)c.toArray()[c.toArray().length-1];

            HashSet<Integer> c_minus_d = (HashSet<Integer>)c.clone();
            c_minus_d.remove(d);

            double rijc = getPartialCorelationValue(i, j, (HashSet<Integer>)c_minus_d.clone());
            double ridc = getPartialCorelationValue(i, d, (HashSet<Integer>)c_minus_d.clone());
            double rjdc = getPartialCorelationValue(j, d, (HashSet<Integer>)c_minus_d.clone());

            double rijdc = (rijc-ridc*rjdc)/sqrt( (1-pow(ridc,2))*(1-pow(rjdc,2)) );

            return rijdc;
        }
    }
    protected Criterion partialCorelations[][];
    public Criterion[][] getPartialCorelations() { return partialCorelations; }
    protected ArrayList<Criterion> significantPartialCorelations = new ArrayList<Criterion>();
    public ArrayList<Criterion> getSignificantPartialCorelations() { return significantPartialCorelations; }
    public void calculatePartialCorelation() {
        final int n = this.getDimension();
        final int N = this.size();
        final int w = n-2; //в нашем случае будет всегда n-2: (0, 1, .., i-1, i+1, .., j-1, j+1, .. n-1)
        final double quantU = Quantiles.CalcU(alpha/2.0);
        final double quantT = Quantiles.t(alpha/2.0,N-w-2);

        HashSet<Integer> c_orig = new HashSet<Integer>();
        for (int i=0; i<n; i++) c_orig.add(i);
        
        significantPartialCorelations.clear();

        for(int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                HashSet<Integer> c = (HashSet<Integer>)c_orig.clone();
                c.remove(i);
                c.remove(j);

//                final int w = c.size();
//                final double quantT = Quantiles.t(alpha/2.0,N-w-2);

                if (partialCorelations[i][j] == null) partialCorelations[i][j] = new Criterion(String.format("r[%d,%d]",i,j));
                Criterion rijc = partialCorelations[i][j];

                rijc.setValue(getPartialCorelationValue(i,j,c));
                double rijc_v = rijc.getValue();
                rijc.setStatistic(rijc_v*sqrt(N-w-2)/sqrt(1-pow(rijc_v,2)));
                rijc.setQuantile(quantT);
                rijc.setSignificant( abs(rijc.getStatistic()) <= rijc.getQuantile() ); //насчет этого я не уверен

                if (rijc.isSignificant()) {
                    //если критерий значим, надо его выводить отдельно с доверительными интервалами
                    double v1 = 0.5* log( (1+rijc_v)/(1-rijc_v) ) - (quantU/(double)(N-w-3));
                    double v2 = 0.5* log( (1+rijc_v)/(1-rijc_v) ) + (quantU/(double)(N-w-3));

                    rijc.setTopConfidienceLimit(    (exp(2*v2)-1)/(exp(2*v2)+1) );
                    rijc.setBottomConfidienceLimit( (exp(2*v1)-1)/(exp(2*v1)+1) );

                    significantPartialCorelations.add(rijc);
                }
                else {
                    rijc.setTopConfidienceLimit(    Double.NaN );
                    rijc.setBottomConfidienceLimit( Double.NaN );                    
                }

            }
        }


    }

    protected Criterion multipleCorelations[];
    public Criterion[] getMultipleCorelations() { return multipleCorelations; }
    public void calculateMultipleCorelations() {
        final int n = this.getDimension();
        final int N = this.size();

        double[][] m = new double[n][n];
        for (int mi=0; mi<n; mi++)
            for(int mii=0; mii<n; mii++)
                m[mi][mii] = pairedCorelations[mi][mii].getValue();
        double delta = new Matrix(m).det();

        for(int i=0; i<n; i++) {
            if (multipleCorelations[i] == null) multipleCorelations[i] = new Criterion(String.format("r[x%d]*",i));
            Criterion rxk = multipleCorelations[i];

            m = new double[n-1][n-1];
            for (int mi=0, mi1=0; mi<(n-1); mi++,mi1++) {
                if (mi1==i) mi1++;
                for(int mii=0, mii1=0; mii<(n-1); mii++, mii1++) {
                    if (mii1==i) mii1++;
                    m[mi][mii] = pairedCorelations[mi1][mii1].getValue();
                }
            }
            double delta1 = new Matrix(m).det();

            rxk.setValue(sqrt(1-delta/delta1));
            rxk.setStatistic( ( (double)(N-n-1)/(double)n ) * ( pow(rxk.getValue(),2)/(1-pow(rxk.getValue(),2)) ) );
            rxk.setQuantile(Quantiles.Fisher(alpha,n,N-n-1));
            rxk.setSignificant(rxk.getStatistic() > rxk.getQuantile());
        }
    }

    protected Criterion regressionCoeficients[];
    public Criterion[] getRegressionCoeficients() { return regressionCoeficients; }
    protected Criterion regressionRestoreCriterion = new Criterion("Regression Restore F-Test");
    public String getFTestString() {
        return String.format("F-Test: %.3f > %.3f = ",regressionRestoreCriterion.getStatistic(), regressionRestoreCriterion.getQuantile())+regressionRestoreCriterion.isSignificant().toString();
    }
    protected Criterion regressionLine[];
    public Criterion[] getRegressionLine() { return regressionLine; }
    public void calculateRegressionCoeficients() {
        final int n = this.getDimension();
        final int N = this.size();
        final double quantT = Quantiles.t(alpha/2.0,N-n);

        double[][] x = new double[N][n-1];
        for(int i=0; i<N; i++)
            for (int j=0; j<(n-1); j++)
                x[i][j] = this.getParam(i,j);
        Matrix X = new Matrix(x);

        double[][] y = new double[N][1];
        for(int i=0; i<N; i++)
            y[i][0] = this.getParam(i, n-1);
        Matrix Y = new Matrix(y);

        Matrix A1 = X.transpose().times(X).inverse();
        Matrix A2 = X.transpose().times(Y);
        Matrix A = A1.times(A2);

        double Rsqr = multipleCorelations[n-1].getValue();

        regressionRestoreCriterion.setStatistic( ((double)(N-n-1)/(double)n)*sqrt( 1.0/(1.0-Rsqr) - 1.0 ) );
        regressionRestoreCriterion.setQuantile(Quantiles.Fisher(alpha, n, N - n - 1));
        regressionRestoreCriterion.setSignificant(regressionRestoreCriterion.getStatistic() > regressionRestoreCriterion.getQuantile());

        double sigma = this.getSigma(n-1)*sqrt((1-Rsqr)*(double)(N-n-1)/(double)(N-1));
        Matrix C = X.transpose().times(X).inverse();

        for (int k=0; k<(n-1); k++) {
            if (regressionCoeficients[k]==null) regressionCoeficients[k] = new Criterion(String.format("a[%d]",k));

            Criterion ak = regressionCoeficients[k];
            ak.setValue(A.get(k,0));
            double ckk = C.get(k, k);
            ak.setDispersion(pow(sigma,2)*ckk);
            ak.setBottomConfidienceLimit(ak.getValue() - quantT*sigma*sqrt(ckk));
            ak.setTopConfidienceLimit(   ak.getValue() + quantT*sigma*sqrt(ckk));
            ak.setStandartized(ak.getValue()*this.getSigma(k)/this.getSigma(n-1));
            if (regressionRestoreCriterion.isSignificant()) {
                ak.setStatistic( ak.getValue()/(sigma*sqrt(C.get(k,k))) );
                ak.setQuantile(quantT);
                ak.setSignificant( !(abs(ak.getStatistic()) <= quantT) );
            }
            else {
                ak.setStatistic(Double.NaN);
                ak.setQuantile(Double.NaN);
                ak.setSignificant(false);
            }
        }

        // линия регресии
        regressionLine = new Criterion[N];
        for (int i=0; i<N; i++) {
            regressionLine[i] = new Criterion(String.format("y[%d]",i));
            Criterion yx = regressionLine[i];
            double yx_v = 0;
            for(int j=0; j<(n-1); j++) yx_v += this.getParam(i, j)*regressionCoeficients[j].getValue();
            yx.setValue(yx_v);
            Matrix Xi = X.getMatrix(i, i, 0, X.getColumnDimension()-1);
            Matrix XA = Xi.times(A);
            Matrix XCXT = Xi.times(C).times(Xi.transpose());
            yx.setStatistic(XA.get(0, 0)-yx.getValue()/(sigma*sqrt(1+XCXT.get(0,0))));
            yx.setBottomConfidienceLimit(XA.get(0,0)-quantT*(sigma*sqrt(1+XCXT.get(0,0))));
            yx.setTopConfidienceLimit(XA.get(0,0)+quantT*(sigma*sqrt(1+XCXT.get(0,0))));
        }
    }
}
