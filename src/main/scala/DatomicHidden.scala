package reactivedatomic

trait TxReportHidden {
  def resolve(id: DId)(implicit db: DDatabase): Option[DLong]

  def resolve(id1: DId, id2: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2) )

  def resolve(id1: DId, id2: DId, id3: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2), resolve(id3) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2), resolve(id3), resolve(id4) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId, id18: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17), resolve(id18) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId, id18: DId, id19: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17), resolve(id18), resolve(id19) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId, id18: DId, id19: DId, id20: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17), resolve(id18), resolve(id19), resolve(id20) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId, id18: DId, id19: DId, id20: DId, id21: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17), resolve(id18), resolve(id19), resolve(id20), resolve(id21) )

  def resolve(id1: DId, id2: DId, id3: DId, id4: DId, id5: DId, id6: DId, id7: DId, id8: DId, id9: DId, id10: DId, id11: DId, id12: DId, id13: DId, id14: DId, id15: DId, id16: DId, id17: DId, id18: DId, id19: DId, id20: DId, id21: DId, id22: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1), resolve(id2), resolve(id3), resolve(id4), resolve(id5), resolve(id6), resolve(id7), resolve(id8), resolve(id9), resolve(id10), resolve(id11), resolve(id12), resolve(id13), resolve(id14), resolve(id15), resolve(id16), resolve(id17), resolve(id18), resolve(id19), resolve(id20), resolve(id21), resolve(id22) )




  // With Identified necessary??
  def resolve(id1: Identified, id2: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id), resolve(id3.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified, id18: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id), resolve(id18.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified, id18: Identified, id19: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id), resolve(id18.id), resolve(id19.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified, id18: Identified, id19: Identified, id20: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id), resolve(id18.id), resolve(id19.id), resolve(id20.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified, id18: Identified, id19: Identified, id20: Identified, id21: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id), resolve(id18.id), resolve(id19.id), resolve(id20.id), resolve(id21.id) )

  def resolve(id1: Identified, id2: Identified, id3: Identified, id4: Identified, id5: Identified, id6: Identified, id7: Identified, id8: Identified, id9: Identified, id10: Identified, id11: Identified, id12: Identified, id13: Identified, id14: Identified, id15: Identified, id16: Identified, id17: Identified, id18: Identified, id19: Identified, id20: Identified, id21: Identified, id22: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong], Option[DLong]) = 
      ( resolve(id1.id), resolve(id2.id), resolve(id3.id), resolve(id4.id), resolve(id5.id), resolve(id6.id), resolve(id7.id), resolve(id8.id), resolve(id9.id), resolve(id10.id), resolve(id11.id), resolve(id12.id), resolve(id13.id), resolve(id14.id), resolve(id15.id), resolve(id16.id), resolve(id17.id), resolve(id18.id), resolve(id19.id), resolve(id20.id), resolve(id21.id), resolve(id22.id) )
}

trait DatomicQueryHidden {
  def query[OutArgs <: Args, T](q: TypedQuery[Args1, OutArgs], d1: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF1[List[T]]).execute(d1)

  def query[OutArgs <: Args, T](q: TypedQuery[Args2, OutArgs], d1: DatomicData, d2: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF2[List[T]]).execute(d1, d2)

  def query[OutArgs <: Args, T](q: TypedQuery[Args3, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF3[List[T]]).execute(d1, d2, d3)

  def query[OutArgs <: Args, T](q: TypedQuery[Args4, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF4[List[T]]).execute(d1, d2, d3, d4)
}

trait ToFunctionImplicitsHidden {

  implicit def toF1[Out] = new ToFunction[Args1, Out] {
    type F[Out] = Function1[DatomicData, Out]
    def convert(f: (Args1 => Out)): F[Out] = (d1: DatomicData) => f(Args1(d1))
  }

  implicit def toF2[Out] = new ToFunction[Args2, Out] {
    type F[Out] = Function2[DatomicData, DatomicData, Out]
    def convert(f: (Args2 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData) => f(Args2(d1, d2)) 
  }

  implicit def toF3[Out] = new ToFunction[Args3, Out] {
    type F[Out] = Function3[DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args3 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData) => f(Args3(d1, d2, d3))
  }

  implicit def toF4[Out] = new ToFunction[Args4, Out] {
    type F[Out] = Function4[DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args4 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData) => f(Args4(d1, d2, d3, d4))
  }

}

trait DatomicDataToArgsImplicitsHidden {
  implicit object DatomicDataToArgs2 extends DatomicDataToArgs[Args2] {
    def toArgs(l: Seq[DatomicData]): Args2 = l match {
      case List(_1, _2) => Args2(_1, _2)
      case _ => throw new RuntimeException("Could convert Seq to Args2")
    }
  }

  implicit def DatomicDataToArgs3 = new DatomicDataToArgs[Args3] {
    def toArgs(l: Seq[DatomicData]): Args3 = l match {
      case List(_1, _2, _3) => Args3(_1, _2, _3)
      case _ => throw new RuntimeException("Could convert Seq to Args3")
    }
  }

  implicit def DatomicDataToArgs4 = new DatomicDataToArgs[Args4] {
    def toArgs(l: Seq[DatomicData]): Args4 = l match {
      case List(_1, _2, _3, _4) => Args4(_1, _2, _3, _4)
      case _ => throw new RuntimeException("Could convert Seq to Args4")
    }
  }
}

trait ArgsToTupleImplicitsHidden {
  implicit def Args2ToTuple = new ArgsToTuple[Args2, (DatomicData, DatomicData)] {
    def convert(from: Args2) = (from._1, from._2)
  }

  implicit def Args3ToTuple = new ArgsToTuple[Args3, (DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args3) = (from._1, from._2, from._3)
  }

  implicit def Args4ToTuple = new ArgsToTuple[Args4, (DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args4) = (from._1, from._2, from._3, from._4)
  }

}

