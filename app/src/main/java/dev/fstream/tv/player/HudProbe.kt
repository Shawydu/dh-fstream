package dev.fstream.tv.player

import org.json.JSONObject

/** Result of the HUD JS probe. */
data class ProbeResult(
    val found: Boolean,
    val paused: Boolean,
    val currentTime: Double,
    val dropped: Long?,
    val total: Long?,
    val crossOrigin: Boolean,
) {
    /** Cross-origin frames hide the real video; only trust a probed video that is actually advancing. */
    val videoVisible get() = found && !(crossOrigin && currentTime <= 0)

    companion object {
        /**
         * Walks top frame and same-origin iframes for the first <video>. Cross-origin iframes
         * (contentDocument inaccessible) set xo=true. evaluateJavascript returns the object as JSON.
         */
        const val JS = """(function(){
  var r={found:false,paused:true,t:0,dropped:-1,total:-1,xo:false};
  function f(d){
    var vs=d.querySelectorAll('video');
    for(var j=0;j<vs.length;j++){
      var v=vs[j];
      if(!r.found || (r.t<=0 && v.currentTime>0)){
        r.found=true; r.paused=v.paused; r.t=v.currentTime;
        try{var q=v.getVideoPlaybackQuality(); r.dropped=q.droppedVideoFrames; r.total=q.totalVideoFrames;}catch(e){}
      }
    }
    var fr=d.querySelectorAll('iframe');
    for(var i=0;i<fr.length;i++){
      try{var c=fr[i].contentDocument; if(c){f(c);}else{r.xo=true;}}catch(e){r.xo=true;}
    }
  }
  f(document);
  return r;
})()"""

        fun parse(json: String?): ProbeResult? {
            if (json.isNullOrEmpty() || json == "null") return null
            return try {
                val o = JSONObject(json)
                ProbeResult(
                    found = o.optBoolean("found"),
                    paused = o.optBoolean("paused", true),
                    currentTime = o.optDouble("t", 0.0),
                    dropped = o.optLong("dropped", -1).takeIf { it >= 0 },
                    total = o.optLong("total", -1).takeIf { it >= 0 },
                    crossOrigin = o.optBoolean("xo"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
