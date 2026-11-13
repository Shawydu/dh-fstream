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
    val ended: Boolean = false,
    val mediaError: Boolean = false,
    val noSource: Boolean = false, // networkState == NETWORK_NO_SOURCE (3)
) {
    /** Cross-origin frames hide the real video; only trust a probed video that is actually advancing. */
    val videoVisible get() = found && !(crossOrigin && currentTime <= 0)

    companion object {
        /** HUD is shown unless a probed video is playing (loading, paused, unknown and error all show it). */
        fun hudVisible(p: ProbeResult?, error: Boolean, always: Boolean): Boolean =
            always || error || p == null || !p.videoVisible || p.paused

        /**
         * Walks top frame and same-origin iframes for the first <video>. Cross-origin iframes
         * (contentDocument inaccessible) set xo=true. evaluateJavascript returns the object as JSON.
         */
        const val JS = """(function(){
  var r={found:false,paused:true,t:0,dropped:-1,total:-1,xo:false,ended:false,err:false,ns:false};
  function f(d){
    var vs=d.querySelectorAll('video');
    for(var j=0;j<vs.length;j++){
      var v=vs[j];
      if(!r.found || (r.t<=0 && v.currentTime>0)){
        r.found=true; r.paused=v.paused; r.t=v.currentTime; r.ended=v.ended; r.err=!!v.error; r.ns=(v.networkState===3);
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
                    ended = o.optBoolean("ended"),
                    mediaError = o.optBoolean("err"),
                    noSource = o.optBoolean("ns"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
