is_index([H] , 0 , H).
is_index([H|_], 0, H).
is_index([_|T] , Index, Element) :- Index > 0, Next is Index - 1, is_index(T, Next , Element).

safe_rout( HX , HY , HX , HY , _ , _ , _ , _ , _ , _ , _ , _ , _ , _, NewArr , Counter , _):-
    is_index(NewArr , Counter , [HX , HY] ).

safe_rout( PX , PY , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , V , W,  NewArr , Counter , Lim  ) :-

    NewW is W + 1,
    StepsLeft is Lim - Counter,
    DHX is abs(PX - HX) - 1 ,
    DHY is abs(PY - HY) - 1,
    (StepsLeft > DHX , StepsLeft > DHY ),
    Counter < Lim  , PY < NewW , PX < NewW,  PX > 0 , PY > 0 ,

    is_index(NewArr , Counter , [PX , PY] ),
    (   ( ( PX == HX , PY == HY) ; ( PX == DX , PY == DY)  ) -> NewV is 1 ; NewV is V  ),

    (   NewV == 1 ;

    DX1 is abs(PX - C1X) ,
    DY1 is abs(PY - C1Y) ,
    \+ (DX1 <2 , DY1 < 2 ),
    DX2 is abs(PX - C2X) ,
    DY2 is abs(PY - C2Y) ,
    \+ (DX2 <2 , DY2 < 2 )
        ),

    PXP is PX+1 , PXM is PX-1 , PYP is PY+1 , PYM is PY-1 , New_c is Counter + 1 ,
    (
    safe_rout( PXP , PY , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c , Lim )
    ;
     safe_rout( PX , PYP , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    ;
    safe_rout( PXP , PYP , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    ;
    safe_rout( PXM , PY , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c , Lim  )
    ;
    safe_rout( PX , PYM , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c , Lim  )
    ;
    safe_rout( PXP , PYP , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    ;
    safe_rout( PXP , PYM , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    ;
    safe_rout( PXM , PYP , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    ;
    safe_rout( PXM , PYM , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , NewV , W, NewArr , New_c, Lim  )
    )
    .
solve(  HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , W, NewArr, A, N ):-

    safe_rout( 1 , 1 , HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y , 0 , W, NewArr , 0 , A ) ; (
                                                             NewA is A + 1,
                                                                A < N ->  (
   solve(  HX , HY, MX , MY , DX , DY , C1X , C1Y , C2X , C2Y  , W, NewArr, NewA , N)

                                                                          )

                                                             ).