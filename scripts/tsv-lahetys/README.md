Apuväline TSV:iden lähettämiseen sähköpostilla HTML-taulukkona
==============================================================

Tarvitaan:
* jq , **vähintään versio 1.6**
* sed
* curl
* JSESSIONID voimassa olevasta sessiosta halutun ympäristön ryhmäsähköpostipalveluun
* tabeilla eroteltu tiedosto

Esim

    ./laheta.bash \
      timo.rantalaiho@reaktor.com \
      "Tässä on kokeeksi sähköpostiviesti, jonka sisältönä on HTML-taulukko" \
      <JSESSIONID> \
      https://virkailija.testiopintopolku.fi \
      < /tmp/Result_1.tsv
